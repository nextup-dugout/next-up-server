package com.nextup.core.domain.game

/**
 * 투수 승/패/세이브/홀드 자동 결정 계산기
 *
 * 경기 종료 시 투수의 Decision을 자동으로 결정합니다.
 *
 * ## 승리 투수
 * - 선발투수가 5이닝 이상 투구하고, 리드를 유지한 채 교체된 경우 → 선발 승
 * - 그 외: 팀이 최종적으로 역전/리드를 확보한 시점에 등판 중이던 구원 투수 → 구원 승
 *
 * ## 패전 투수
 * - 결승점(승리팀이 최종 리드를 확보한 이닝의 점수)을 허용한 시점의 투수
 *
 * ## 세이브
 * 구원 투수(마지막 투수)가 다음 중 하나 충족 시:
 * 1. 3점 이내 리드 상황으로 등판하여 마무리
 * 2. 3이닝 이상 마무리
 *
 * ## 홀드
 * - 리드 상황에 등판하여 리드를 유지한 채 교체된 구원 투수
 *
 * ## 블론세이브
 * - 세이브/홀드 상황에서 등판했으나 동점/역전을 허용한 구원 투수
 *   (이후 팀이 다시 역전해 승리한 경우)
 *
 * ## 무승부
 * - 모든 투수 NONE
 *
 * 이 클래스는 순수 도메인 로직만 담습니다. Repository/Spring 의존 금지.
 */
object PitchingDecisionCalculator {

    /**
     * 경기의 모든 투수 결정을 계산합니다.
     *
     * @param winnerGameTeam 승리팀 GameTeam (무승부 시 null)
     * @param loserGameTeam 패전팀 GameTeam (무승부 시 null)
     * @param allPitchingRecords 경기의 모든 투수 기록 리스트
     * @return 각 투수 기록에 부여할 결정 맵 (PitchingRecord -> PitchingDecision)
     */
    fun calculate(
        winnerGameTeam: GameTeam?,
        loserGameTeam: GameTeam?,
        allPitchingRecords: List<PitchingRecord>,
    ): Map<PitchingRecord, PitchingDecision> {
        // 무승부: 모든 투수 NONE
        if (winnerGameTeam == null || loserGameTeam == null) {
            return emptyMap()
        }

        val result = mutableMapOf<PitchingRecord, PitchingDecision>()

        val winnerTeamId = winnerGameTeam.team.id
        val loserTeamId = loserGameTeam.team.id

        // 팀별 투수 기록 분리 (등판 순서: entryInning ASC, isStartingPitcher DESC)
        val winnerPitchers =
            allPitchingRecords
                .filter { it.gamePlayer.gameTeam.team.id == winnerTeamId }
                .sortedWith(
                    compareBy<PitchingRecord> { it.gamePlayer.entryInning ?: 1 }
                        .thenByDescending { if (it.isStartingPitcher) 1 else 0 },
                )

        val loserPitchers =
            allPitchingRecords
                .filter { it.gamePlayer.gameTeam.team.id == loserTeamId }
                .sortedWith(
                    compareBy<PitchingRecord> { it.gamePlayer.entryInning ?: 1 }
                        .thenByDescending { if (it.isStartingPitcher) 1 else 0 },
                )

        // 이닝별 점수 파싱 (GameTeam.inningScores: "0,1,2,0,3" 형식, 각 이닝 득점)
        val winnerInningScores = parseInningScores(winnerGameTeam.inningScores)
        val loserInningScores = parseInningScores(loserGameTeam.inningScores)

        // 이닝별 누적 점수 계산
        val winnerCumulative = toCumulative(winnerInningScores)
        val loserCumulative = toCumulative(loserInningScores)

        val finalWinnerScore = winnerGameTeam.totalScore
        val finalLoserScore = loserGameTeam.totalScore
        val finalLeadMargin = finalWinnerScore - finalLoserScore

        // 승리팀 투수 결정
        calculateWinnerPitchersDecisions(
            winnerPitchers = winnerPitchers,
            winnerCumulative = winnerCumulative,
            loserCumulative = loserCumulative,
            finalLeadMargin = finalLeadMargin,
            result = result,
        )

        // 패전팀 투수 결정
        calculateLoserPitchersDecisions(
            loserPitchers = loserPitchers,
            winnerCumulative = winnerCumulative,
            loserCumulative = loserCumulative,
            result = result,
        )

        return result
    }

    // ── 승리팀 투수 결정 ──────────────────────────────────────────────────────

    private fun calculateWinnerPitchersDecisions(
        winnerPitchers: List<PitchingRecord>,
        winnerCumulative: List<Int>,
        loserCumulative: List<Int>,
        finalLeadMargin: Int,
        result: MutableMap<PitchingRecord, PitchingDecision>,
    ) {
        if (winnerPitchers.isEmpty()) return

        // 팀이 최종 리드를 잡은 이닝 (1-indexed, 없으면 1회)
        val finalLeadInning = findFinalLeadChangeInning(winnerCumulative, loserCumulative) ?: 1

        // 승리 투수 결정
        val winRecord =
            determineWinRecord(
                winnerPitchers = winnerPitchers,
                winnerCumulative = winnerCumulative,
                loserCumulative = loserCumulative,
                finalLeadInning = finalLeadInning,
            )

        // 마지막 투수 세이브 자격
        val lastPitcher = winnerPitchers.last()
        val isLastPitcherWin = (lastPitcher == winRecord)
        val saveRecord =
            if (!isLastPitcherWin && winnerPitchers.size > 1) {
                val lp = winnerPitchers.last()
                if (qualifiesForSave(lp, finalLeadMargin)) lp else null
            } else {
                null
            }

        // 중간 구원 투수들 홀드/블론세이브
        val middleRange =
            if (winnerPitchers.size > 2) {
                winnerPitchers.subList(1, winnerPitchers.size - 1)
            } else {
                emptyList()
            }

        for (record in middleRange) {
            if (record == winRecord) continue
            val decision =
                determineMiddleReliefDecision(
                    record = record,
                    winnerCumulative = winnerCumulative,
                    loserCumulative = loserCumulative,
                )
            result[record] = decision
        }

        // 기록 최종 할당
        if (winRecord != null) result[winRecord] = PitchingDecision.WIN
        if (saveRecord != null) result[saveRecord] = PitchingDecision.SAVE
    }

    /**
     * 승리 투수 기록을 결정합니다.
     *
     * 1. 선발투수 5이닝+ & 교체 시점 리드 → 선발 승
     * 2. 그 외 → 최종 리드 확보 이닝의 구원 투수 → 구원 승
     */
    private fun determineWinRecord(
        winnerPitchers: List<PitchingRecord>,
        winnerCumulative: List<Int>,
        loserCumulative: List<Int>,
        finalLeadInning: Int,
    ): PitchingRecord? {
        if (winnerPitchers.isEmpty()) return null

        val starter = winnerPitchers.first()

        // 선발 승 자격: 5이닝 이상 & 교체 시점에 리드
        if (starter.isStartingPitcher && starter.inningsPitchedOuts >= 15) {
            val starterLastInning = starter.completeInnings
            val winnerScoreAtExit = winnerCumulative.getOrElse(starterLastInning - 1) { 0 }
            val loserScoreAtExit = loserCumulative.getOrElse(starterLastInning - 1) { 0 }
            if (winnerScoreAtExit > loserScoreAtExit) {
                return starter
            }
        }

        // 구원 승: 최종 리드 확보 이닝의 투수
        return findPitcherAtInning(winnerPitchers, finalLeadInning)
    }

    /**
     * 세이브 자격 판단.
     *
     * 조건 중 하나 충족:
     * 1. 3점 이내 리드 상황으로 마무리
     * 2. 3이닝 이상 던지며 마무리
     */
    private fun qualifiesForSave(
        record: PitchingRecord,
        finalLeadMargin: Int,
    ): Boolean {
        if (record.isStartingPitcher) return false
        if (finalLeadMargin <= 0) return false
        return finalLeadMargin <= 3 || record.completeInnings >= 3
    }

    /**
     * 중간 구원 투수의 결정을 계산합니다.
     *
     * - 등판 시점에 리드 중이고 교체 시점도 리드 → HOLD
     * - 등판 시점 리드였으나 동점/역전 허용 후 팀이 재역전 → BLOWN_SAVE
     * - 그 외 → NONE
     *
     * entryInning은 투수가 등판한 이닝이며, 그 이닝이 시작되기 전 점수로 리드 여부를 판단합니다.
     * 즉, entryInning 시작 전 점수 = winnerCumulative[entryInning - 2] (이전 이닝까지 누적)
     */
    private fun determineMiddleReliefDecision(
        record: PitchingRecord,
        winnerCumulative: List<Int>,
        loserCumulative: List<Int>,
    ): PitchingDecision {
        val entryInning = record.gamePlayer.entryInning ?: 1
        val exitInning = record.gamePlayer.exitInning

        // 등판 이닝 시작 전 누적 점수 (entryInning=1이면 0:0)
        val winnerBeforeEntry = if (entryInning > 1) winnerCumulative.getOrElse(entryInning - 2) { 0 } else 0
        val loserBeforeEntry = if (entryInning > 1) loserCumulative.getOrElse(entryInning - 2) { 0 } else 0
        val enteredWithLead = winnerBeforeEntry > loserBeforeEntry

        if (!enteredWithLead) return PitchingDecision.NONE

        // 등판 이후 교체 이닝까지 리드가 유지됐는지 확인
        val endInning = exitInning ?: (entryInning + record.completeInnings - 1).coerceAtLeast(entryInning)
        var leadLost = false
        for (inning in entryInning..endInning) {
            val w = winnerCumulative.getOrElse(inning - 1) { 0 }
            val l = loserCumulative.getOrElse(inning - 1) { 0 }
            if (w <= l) {
                leadLost = true
                break
            }
        }

        return if (leadLost) PitchingDecision.BLOWN_SAVE else PitchingDecision.HOLD
    }

    // ── 패전팀 투수 결정 ──────────────────────────────────────────────────────

    private fun calculateLoserPitchersDecisions(
        loserPitchers: List<PitchingRecord>,
        winnerCumulative: List<Int>,
        loserCumulative: List<Int>,
        result: MutableMap<PitchingRecord, PitchingDecision>,
    ) {
        if (loserPitchers.isEmpty()) return

        val decidingInning =
            findFinalLeadChangeInning(winnerCumulative, loserCumulative)
                ?: winnerCumulative.size.coerceAtLeast(1)

        val loserRecord =
            findPitcherAtInning(loserPitchers, decidingInning)
                ?: loserPitchers.last()

        result[loserRecord] = PitchingDecision.LOSS
    }

    // ── 헬퍼 함수 ─────────────────────────────────────────────────────────────

    /**
     * GameTeam.inningScores 문자열을 파싱합니다.
     * 형식: "0,1,2,0,3" (각 이닝 득점, 1회=index 0)
     */
    internal fun parseInningScores(inningScores: String?): List<Int> {
        if (inningScores.isNullOrBlank()) return emptyList()
        return inningScores.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    /**
     * 이닝별 득점을 누적 점수로 변환합니다.
     * 예: [0, 1, 2, 0, 3] → [0, 1, 3, 3, 6]
     */
    internal fun toCumulative(perInning: List<Int>): List<Int> {
        var sum = 0
        return perInning.map { score ->
            sum += score
            sum
        }
    }

    /**
     * 팀이 최종적으로 리드를 확보한 이닝 번호를 반환합니다 (1-indexed).
     * 이후에 동점/역전 없이 경기가 종료된 마지막 역전 이닝.
     *
     * "리드 변화"란 이전 이닝까지 리드하지 않다가 해당 이닝 종료 후 리드를 잡은 경우입니다.
     * 처음부터(0:0) 리드를 유지한 경우는 리드 변화가 없으므로 null 반환.
     */
    internal fun findFinalLeadChangeInning(
        winnerCumulative: List<Int>,
        loserCumulative: List<Int>,
    ): Int? {
        val totalInnings = maxOf(winnerCumulative.size, loserCumulative.size)
        var finalLeadChangeInning: Int? = null

        for (inning in 1..totalInnings) {
            val w = winnerCumulative.getOrElse(inning - 1) { 0 }
            val l = loserCumulative.getOrElse(inning - 1) { 0 }
            // 이전 이닝 종료 후 누적 점수 (이닝 1 이전은 0:0)
            val prevW = if (inning > 1) winnerCumulative.getOrElse(inning - 2) { 0 } else 0
            val prevL = if (inning > 1) loserCumulative.getOrElse(inning - 2) { 0 } else 0

            // 이전에 리드하지 않았고(동점 또는 뒤처짐), 이번 이닝에 리드 확보
            // 단, 이닝 1에서 0:0 → 1:0이 되는 것도 리드 변화로 포함
            val wasNotLeading = prevW <= prevL
            val isNowLeading = w > l

            // 경기 시작 전 상태(0:0)에서 첫 이닝에 바로 리드 잡는 것은 리드 '변화'가 아님
            // → 이전 이닝에 동점(prevW == prevL)이면서 winner가 앞선 경우만 리드 변화로 인정
            // → prevW < prevL (뒤처짐) → 역전, 혹은 prevW == prevL (동점) → 리드 확보
            if (wasNotLeading && isNowLeading) {
                // 게임 시작(0:0) → 첫 이닝 리드는 역전이 아니라 최초 리드
                // 이것도 포함: "처음 리드를 잡은 이닝"도 승리 투수 결정에 필요
                finalLeadChangeInning = inning
            }
        }

        // 처음 이닝부터 계속 리드를 유지한 경우:
        // finalLeadChangeInning == 1이고 이후 한 번도 역전이 없으면 null 반환
        // (선발 투수 승리 자격 판별 시 "처음부터 리드"이면 null이어도 선발 승 처리)
        if (finalLeadChangeInning == 1) {
            // 이닝 1 이후에도 계속 리드했는지 확인: 역전된 이닝이 있었는지
            val hadLaterReversal =
                (2..totalInnings).any { inning ->
                    val w = winnerCumulative.getOrElse(inning - 1) { 0 }
                    val l = loserCumulative.getOrElse(inning - 1) { 0 }
                    val prevW = winnerCumulative.getOrElse(inning - 2) { 0 }
                    val prevL = loserCumulative.getOrElse(inning - 2) { 0 }
                    // 이전에 리드 중이었는데 이번 이닝에 동점/역전 당했다가 다시 리드하는 경우
                    prevW > prevL && w <= l
                }
            if (!hadLaterReversal) return null
        }

        return finalLeadChangeInning
    }

    /**
     * 특정 이닝(1-indexed)에 등판 중이던 투수 기록을 찾습니다.
     * GamePlayer.entryInning / exitInning 을 기준으로 판단합니다.
     * entryInning이 없는 경우 등판 순서에서 이닝 범위를 추정합니다.
     */
    private fun findPitcherAtInning(
        pitchers: List<PitchingRecord>,
        inning: Int,
    ): PitchingRecord? {
        // entryInning/exitInning이 있는 경우 우선 사용
        val byEntry =
            pitchers.firstOrNull { record ->
                val entry = record.gamePlayer.entryInning ?: return@firstOrNull false
                val exit = record.gamePlayer.exitInning
                entry <= inning && (exit == null || exit >= inning)
            }
        if (byEntry != null) return byEntry

        // entryInning 정보가 없으면 아웃 수 누산으로 이닝 범위 추정
        var cumulativeOuts = 0
        for (record in pitchers) {
            val startInning = (cumulativeOuts / 3) + 1
            cumulativeOuts += record.inningsPitchedOuts
            val endInning = (cumulativeOuts / 3) + (if (cumulativeOuts % 3 > 0) 1 else 0)
            if (inning in startInning..endInning) {
                return record
            }
        }

        return pitchers.lastOrNull()
    }
}
