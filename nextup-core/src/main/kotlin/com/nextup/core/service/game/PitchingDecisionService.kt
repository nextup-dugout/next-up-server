package com.nextup.core.service.game

import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import org.springframework.stereotype.Service

/**
 * 투수 승/패/세이브/홀드 자동 결정 도메인 서비스
 *
 * 경기 종료 시 각 팀의 투수 기록 목록과 이닝별 점수를 분석하여
 * 표준 야구 규칙(사회인 야구 규칙 포함)에 따라 Decision을 자동 할당합니다.
 *
 * ## 규칙 요약
 * - **승리 투수 (W)**:
 *   - 선발: `starterWinQualificationOuts` 이상 소화 + 리드한 채 교체 (기본 15아웃=5이닝)
 *   - 구원: 리드를 만들거나 지키며 교체된 투수 중 마지막으로 역전/동점→리드를 만든 투수
 * - **패전 투수 (L)**: 패배팀의 결승점을 허용한 시점의 투수
 * - **세이브 (S)**: 리드 상황 구원 등판 후 경기 종료까지 아웃을 잡고 다음 조건 중 하나 충족
 *   - 3점 이내 리드 상황 등판
 *   - 동점 주자를 상대
 *   - 3이닝 이상 마무리
 * - **홀드 (H)**: 리드 상황 구원 등판 → 리드 유지하고 교체
 * - **블론세이브 (BS)**: 세이브 상황 등판 후 동점/역전 허용
 *
 * ## 이 서비스의 동작 범위
 * - 모든 투수의 누적 이닝별 점수 맥락은 `inningScores` 기반으로 시뮬레이션
 * - 이닝별 리드 변화 추적이 불가할 경우, 단순화된 최종 점수 기반 규칙 적용
 * - 서비스는 `PitchingRecord`의 비즈니스 메서드(`assignWin`, `assignLoss` 등)를 호출하여 결정 위임
 */
@Service
class PitchingDecisionService {

    /**
     * 경기 종료 시 투수 Decision을 자동으로 결정하고 할당합니다.
     *
     * @param winnerTeamRecords 승리팀 투수 기록 목록 (isStartingPitcher 순서로 정렬됨)
     * @param loserTeamRecords  패배팀 투수 기록 목록
     * @param winnerGameTeam    승리팀 GameTeam (이닝별 점수 조회용)
     * @param loserGameTeam     패배팀 GameTeam (이닝별 점수 조회용)
     * @param gameRules         경기 규칙 (선발 승리 자격 이닝 등 사회인 야구 설정 포함)
     */
    fun assignDecisions(
        winnerTeamRecords: List<PitchingRecord>,
        loserTeamRecords: List<PitchingRecord>,
        winnerGameTeam: GameTeam,
        loserGameTeam: GameTeam,
        gameRules: GameRules,
    ) {
        // 투수 기록이 없으면 결정 불가
        if (winnerTeamRecords.isEmpty() && loserTeamRecords.isEmpty()) return

        assignWinAndRelief(winnerTeamRecords, winnerGameTeam, loserGameTeam, gameRules)
        assignLoss(loserTeamRecords, winnerGameTeam, loserGameTeam, gameRules)
    }

    // ─────────────────────────────────────────────────────────────
    // 승리 관련 결정 (W / S / H / BS)
    // ─────────────────────────────────────────────────────────────

    private fun assignWinAndRelief(
        winnerRecords: List<PitchingRecord>,
        winnerGameTeam: GameTeam,
        loserGameTeam: GameTeam,
        gameRules: GameRules,
    ) {
        if (winnerRecords.isEmpty()) return

        val starter = winnerRecords.firstOrNull { it.isStartingPitcher }
        val relievers = winnerRecords.filter { !it.isStartingPitcher }

        val winnerInningScores = parseInningScores(winnerGameTeam.inningScores)
        val loserInningScores = parseInningScores(loserGameTeam.inningScores)
        val finalWinnerScore = winnerGameTeam.totalScore
        val finalLoserScore = loserGameTeam.totalScore

        // 선발 승리 여부 판정
        val starterEligibleForWin =
            starter != null &&
                starter.inningsPitchedOuts >= gameRules.starterWinQualificationOuts

        if (starterEligibleForWin && relievers.isEmpty()) {
            // 완투: 선발에게 승리
            starter!!.assignWin()
            return
        }

        if (starterEligibleForWin && relievers.isNotEmpty()) {
            // 선발이 자격을 갖추고 구원이 있는 경우:
            // 선발이 리드한 채로 교체됐는지 확인
            val starterExitInning = estimateStarterExitInning(starter!!)
            val leadAfterStarter =
                isLeadingAfterInning(
                    starterExitInning,
                    winnerInningScores,
                    loserInningScores,
                )

            if (leadAfterStarter) {
                starter.assignWin()
                // 마지막 구원투수가 리드를 유지하며 경기를 마쳤으면 세이브/홀드
                assignReliefDecisions(relievers, finalWinnerScore, finalLoserScore, isSaveEligible = true)
                return
            }
            // 선발이 리드를 잃은 상태로 교체: 구원 투수 중 승리 투수 결정
        }

        // 구원 투수 중 승리 투수 결정
        if (relievers.isNotEmpty()) {
            val starterOuts = starter?.inningsPitchedOuts ?: 0
            val winningRelief =
                findWinningReliever(relievers, winnerInningScores, loserInningScores, starterOuts)
            winningRelief?.assignWin()

            val remainingRelievers =
                if (winningRelief != null) {
                    relievers.filter { it != winningRelief }
                } else {
                    relievers
                }

            // 마지막 구원투수: 세이브 가능성 판정
            assignReliefDecisions(
                relievers = remainingRelievers,
                finalWinnerScore = finalWinnerScore,
                finalLoserScore = finalLoserScore,
                isSaveEligible = winningRelief != null,
            )
        } else if (!starterEligibleForWin && starter != null) {
            // 구원 없고 선발 자격 미달: 그냥 승리 부여 (완투이지만 자격 이닝 미달)
            // 사회인 야구 등 단축 경기에서 발생 가능
            starter.assignWin()
        }
    }

    /**
     * 구원 투수들에게 홀드/세이브/블론세이브를 부여합니다.
     *
     * - 마지막 구원투수 = 세이브 or 블론세이브
     * - 중간 구원투수들 = 홀드 or 블론세이브
     *
     * @param runnersOnBase 마무리 투수 등판 시점의 주자 수 (동점 주자 세이브 조건 판단용)
     */
    private fun assignReliefDecisions(
        relievers: List<PitchingRecord>,
        finalWinnerScore: Int,
        finalLoserScore: Int,
        isSaveEligible: Boolean,
        runnersOnBase: Int = 0,
    ) {
        if (relievers.isEmpty()) return

        val lastRelief = relievers.last()
        val middleRelievers = relievers.dropLast(1)

        // 중간 구원 투수 처리
        for (r in middleRelievers) {
            if (r.decision == PitchingDecision.NONE) {
                // 블론세이브 여부는 마지막 투수에서만 정확히 판단 가능,
                // 중간 투수들은 홀드 기본 부여
                r.assignHold()
            }
        }

        // 마지막 구원 투수 처리
        if (lastRelief.decision == PitchingDecision.NONE && isSaveEligible) {
            val leadMargin = finalWinnerScore - finalLoserScore
            val isSaveSituation =
                qualifiesForSave(lastRelief, leadMargin, runnersOnBase)

            if (isSaveSituation) {
                lastRelief.assignSave()
            }
            // 블론세이브는 패배팀 투수 분석에서 처리됨 (이 투수들은 승리팀)
        }
    }

    /**
     * 세이브 자격을 판단합니다.
     *
     * 다음 조건 중 하나를 충족하면 세이브 자격:
     * 1. 3점 이내 리드 상황으로 등판하여 마무리
     * 2. 3이닝 이상 던지며 마무리
     * 3. 동점 주자를 상대 (리드 점수 <= 주자 수 + 1, 즉 동점 타자가 타석/온베이스/다음 타자)
     *
     * @param record 마무리 투수 기록
     * @param finalLeadMargin 최종 리드 점수 차이
     * @param runnersOnBase 등판 시점의 주자 수
     */
    internal fun qualifiesForSave(
        record: PitchingRecord,
        finalLeadMargin: Int,
        runnersOnBase: Int = 0,
    ): Boolean {
        if (record.isStartingPitcher) return false
        if (finalLeadMargin <= 0) return false
        return finalLeadMargin <= 3 ||
            isSaveByInnings(record) ||
            finalLeadMargin <= runnersOnBase + 1
    }

    /**
     * 3이닝 이상 던진 마무리 투수 조건 (세이브 조건 중 하나).
     */
    private fun isSaveByInnings(record: PitchingRecord): Boolean = record.inningsPitchedOuts >= 9

    // ─────────────────────────────────────────────────────────────
    // 패전 관련 결정 (L / BS)
    // ─────────────────────────────────────────────────────────────

    private fun assignLoss(
        loserRecords: List<PitchingRecord>,
        winnerGameTeam: GameTeam,
        loserGameTeam: GameTeam,
        gameRules: GameRules,
    ) {
        if (loserRecords.isEmpty()) return

        val winnerInningScores = parseInningScores(winnerGameTeam.inningScores)
        val loserInningScores = parseInningScores(loserGameTeam.inningScores)

        // 블론세이브를 먼저 처리: 리드를 잃은 시점의 투수에게 BS 부여 (LOSS보다 우선)
        assignBlownSaves(loserRecords, winnerInningScores, loserInningScores)

        // 패전 투수 결정: BS가 이미 부여된 투수는 LOSS에서 제외
        val losingPitcher = findLosingPitcher(loserRecords, winnerInningScores, loserInningScores)
        losingPitcher?.let {
            if (it.decision == PitchingDecision.NONE) {
                it.assignLoss()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼: 승리 투수 / 패전 투수 결정 로직
    // ─────────────────────────────────────────────────────────────

    /**
     * 구원 투수 목록에서 "역전 또는 동점→리드를 만든" 시점의 투수를 찾습니다.
     *
     * 단순화: 이닝별 누적 점수로 팀 리드 변화를 추적합니다.
     * 가장 처음으로 리드가 만들어진 이닝에서 투구한 구원 투수를 반환합니다.
     *
     * @param starterOuts 선발 투수가 소화한 아웃 수 (구원 투수의 이닝 오프셋 계산용)
     */
    private fun findWinningReliever(
        relievers: List<PitchingRecord>,
        winnerInningScores: List<Int>,
        loserInningScores: List<Int>,
        starterOuts: Int = 0,
    ): PitchingRecord? {
        if (relievers.isEmpty()) return null

        // 이닝별 누적 점수 계산
        val winnerCumulative = cumulativeScores(winnerInningScores)
        val loserCumulative = cumulativeScores(loserInningScores)

        // 최초로 리드를 잡은 이닝 탐색
        var firstLeadInning = -1
        for (i in winnerCumulative.indices) {
            val w = winnerCumulative[i]
            val l = loserCumulative.getOrElse(i) { 0 }
            if (w > l) {
                firstLeadInning = i + 1 // 1-indexed
                break
            }
        }

        if (firstLeadInning < 0) {
            // 리드를 잡은 이닝을 특정할 수 없으면 마지막 구원투수에게 승리
            return relievers.last()
        }

        // 구원 투수의 이닝은 선발 투수 다음부터 시작
        // starterOuts를 offset으로 빼서 구원 투수 기준 상대 이닝으로 변환
        val starterInnings = starterOuts / 3
        val relieverTargetInning = (firstLeadInning - starterInnings).coerceAtLeast(1)

        return findRelieverAtInning(relievers, relieverTargetInning) ?: relievers.last()
    }

    /**
     * 패배팀 투수 중 결승점을 허용한 투수를 찾습니다.
     *
     * 결승점(go-ahead run): 승리팀이 최종적으로 역전하지 못할 리드를 잡은 시점의 득점.
     * 해당 이닝에서 투구한 패배팀 투수에게 패전을 부여합니다.
     *
     * 결승점 판정 로직:
     * 1. 이닝별 누적 점수를 역순으로 탐색하여, 승리팀이 최종 점수에 도달하기 직전
     *    마지막으로 리드를 확보한 이닝을 찾습니다.
     * 2. 승리팀이 동점 또는 뒤진 상태에서 리드를 잡은 마지막 시점이 결승점 이닝입니다.
     */
    private fun findLosingPitcher(
        loserRecords: List<PitchingRecord>,
        winnerInningScores: List<Int>,
        loserInningScores: List<Int>,
    ): PitchingRecord? {
        if (loserRecords.isEmpty()) return null

        val winnerCumulative = cumulativeScores(winnerInningScores)
        val loserCumulative = cumulativeScores(loserInningScores)

        if (winnerCumulative.isEmpty()) {
            return loserRecords.maxByOrNull { it.runsAllowed } ?: loserRecords.last()
        }

        // 결승점 이닝: 승리팀이 마지막으로 동점 또는 뒤진 상태에서 리드를 잡은 이닝
        var decisionInning = -1
        for (i in winnerCumulative.indices) {
            val w = winnerCumulative[i]
            val l = loserCumulative.getOrElse(i) { 0 }
            val prevW = if (i > 0) winnerCumulative[i - 1] else 0
            val prevL = if (i > 0) loserCumulative.getOrElse(i - 1) { 0 } else 0

            // 이 이닝에서 승리팀이 리드를 (재)확보한 경우
            if (w > l && prevW <= prevL) {
                decisionInning = i + 1 // 1-indexed
            }
        }

        if (decisionInning < 0) {
            // 결승점 이닝 특정 불가: 가장 많은 실점을 허용한 투수
            return loserRecords.maxByOrNull { it.runsAllowed } ?: loserRecords.last()
        }

        return findRelieverAtInning(loserRecords, decisionInning) ?: loserRecords.last()
    }

    /**
     * 블론세이브 할당: 패배팀 구원 투수가 세이브 상황에서 등판 후 리드를 잃은 경우.
     */
    private fun assignBlownSaves(
        loserRecords: List<PitchingRecord>,
        winnerInningScores: List<Int>,
        loserInningScores: List<Int>,
    ) {
        val relievers = loserRecords.filter { !it.isStartingPitcher }
        if (relievers.isEmpty()) return

        // 패배팀이 리드를 잡았다가 잃은 구간 탐색
        val loserCumulative = cumulativeScores(loserInningScores)
        val winnerCumulative = cumulativeScores(winnerInningScores)

        var hadLead = false
        var lostLeadInning = -1

        for (i in loserCumulative.indices) {
            val l = loserCumulative[i]
            val w = winnerCumulative.getOrElse(i) { 0 }
            if (l > w) hadLead = true
            if (hadLead && w >= l && lostLeadInning < 0) {
                lostLeadInning = i + 1 // 1-indexed
            }
        }

        if (hadLead && lostLeadInning > 0) {
            val blownRelief = findRelieverAtInning(relievers, lostLeadInning) ?: relievers.last()
            // BS는 패배팀 구원 투수에게 부여: LOSS보다 먼저 처리되므로 NONE 체크 불필요
            blownRelief.assignBlownSave()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼: 이닝 기반 투수 매핑
    // ─────────────────────────────────────────────────────────────

    /**
     * 특정 이닝에서 투구한 투수를 찾습니다.
     *
     * 투수 목록의 순서(등판 순서)와 각 투수의 아웃 수로 이닝 범위를 추정합니다.
     * 각 투수가 소화한 아웃을 누적하여 해당 이닝에 걸리는 투수를 반환합니다.
     */
    private fun findRelieverAtInning(
        records: List<PitchingRecord>,
        targetInning: Int,
    ): PitchingRecord? {
        if (records.isEmpty()) return null

        var cumulativeOuts = 0
        for (record in records) {
            cumulativeOuts += record.inningsPitchedOuts
            // 누적 아웃 수로 소화 이닝 수 계산
            // 예: 3아웃 → 1이닝, 4아웃 → 2이닝(시작), 9아웃 → 3이닝
            // (cumulativeOuts - 1) / 3 + 1: 0 아웃 예외 처리 포함
            val coveredInnings =
                if (cumulativeOuts == 0) 0 else (cumulativeOuts - 1) / 3 + 1
            if (coveredInnings >= targetInning) {
                return record
            }
        }
        return records.last()
    }

    /**
     * 선발 투수가 소화한 이닝 수로 퇴장 이닝을 추정합니다.
     * (completeInnings: 완전 이닝 수 기준)
     */
    private fun estimateStarterExitInning(starter: PitchingRecord): Int = starter.completeInnings.coerceAtLeast(1)

    /**
     * 특정 이닝까지 누적 점수 비교 시 승리팀이 앞서고 있는지 확인합니다.
     *
     * @param inning            비교 기준 이닝 (1-indexed)
     * @param winnerInningScores 승리팀 이닝별 점수
     * @param loserInningScores  패배팀 이닝별 점수
     */
    private fun isLeadingAfterInning(
        inning: Int,
        winnerInningScores: List<Int>,
        loserInningScores: List<Int>,
    ): Boolean {
        val winnerScore = winnerInningScores.take(inning).sum()
        val loserScore = loserInningScores.take(inning).sum()
        return winnerScore > loserScore
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼: 점수 파싱
    // ─────────────────────────────────────────────────────────────

    /**
     * "0,1,2,0,3" 형태의 이닝 점수 문자열을 Int 리스트로 변환합니다.
     */
    private fun parseInningScores(inningScores: String?): List<Int> {
        if (inningScores.isNullOrBlank()) return emptyList()
        return inningScores.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    /**
     * 이닝별 점수 목록을 이닝별 누적 점수로 변환합니다.
     * 예: [1, 0, 2] → [1, 1, 3]
     */
    private fun cumulativeScores(inningScores: List<Int>): List<Int> {
        var sum = 0
        return inningScores.map { score ->
            sum += score
            sum
        }
    }
}
