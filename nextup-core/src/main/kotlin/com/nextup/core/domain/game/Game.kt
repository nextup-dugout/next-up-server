package com.nextup.core.domain.game

import com.nextup.common.exception.GameAlreadyLockedException
import com.nextup.common.exception.GameNotLockedByCurrentScorerException
import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.team.Team
import jakarta.persistence.*
import java.time.Duration
import java.time.LocalDateTime

/**
 * 경기 엔티티
 *
 * 대회에 속한 개별 경기를 나타냅니다.
 * 경기에 참여하는 팀(GameTeam)과 선수(GamePlayer)는
 * 각각 ManyToOne으로 Game을 참조합니다.
 */
@Entity
@Table(
    name = "games",
    indexes = [
        Index(name = "idx_games_competition", columnList = "competition_id"),
        Index(name = "idx_games_scheduled_at", columnList = "scheduled_at"),
        Index(name = "idx_games_status", columnList = "status"),
        Index(name = "idx_games_competition_date", columnList = "competition_id, scheduled_at"),
    ],
)
class Game private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    val competition: Competition,
    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: LocalDateTime,
    @Column(length = 100)
    var location: String? = null,
    @Column(name = "field_name", length = 100)
    var fieldName: String? = null,
    @Column(name = "game_number")
    var gameNumber: Int? = null,
    @Column(name = "is_doubleheader", nullable = false)
    var isDoubleheader: Boolean = false,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: GameStatus = GameStatus.SCHEDULED,
    @Column(name = "current_inning")
    var currentInning: Int = 0,
    @Column(name = "is_top_inning")
    var isTopInning: Boolean = true,
    @Column(name = "total_innings")
    var totalInnings: Int = 9,
    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null,
    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,
    @Column(length = 500)
    var note: String? = null,
    @Column(name = "forfeit_reason", length = 500)
    var forfeitReason: String? = null,
    @Embedded
    var gameState: GameState = GameState(),
    @Column(name = "scorer_id")
    var scorerId: Long? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Version
    var version: Long = 0

    @OneToMany(mappedBy = "game", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _gameTeams: MutableList<GameTeam> = mutableListOf()
    val gameTeams: List<GameTeam> get() = _gameTeams.toList()

    /**
     * 경기를 시작합니다.
     */
    fun start() {
        require(status.canStart()) { "예정 상태의 경기만 시작할 수 있습니다. 현재 상태: ${status.displayName}" }
        status = GameStatus.IN_PROGRESS
        currentInning = 1
        isTopInning = true
        startedAt = LocalDateTime.now()
        gameState.resetForNewInning()
    }

    /**
     * 기록원이 경기를 독점 잠금합니다.
     *
     * 이미 다른 기록원이 잠금한 경우 [GameAlreadyLockedException]이 발생합니다.
     * 동일 기록원이 중복 잠금 시도하면 멱등하게 무시합니다.
     *
     * @param scorerId 잠금을 요청하는 기록원 ID
     * @throws GameAlreadyLockedException 다른 기록원이 이미 잠금한 경우
     */
    fun lockForScorer(scorerId: Long) {
        if (this.scorerId != null && this.scorerId != scorerId) {
            throw GameAlreadyLockedException(id, this.scorerId!!)
        }
        this.scorerId = scorerId
    }

    /**
     * 기록원의 경기 잠금을 해제합니다.
     *
     * 잠금한 기록원 본인만 해제할 수 있습니다.
     *
     * @param scorerId 잠금 해제를 요청하는 기록원 ID
     * @throws GameNotLockedByCurrentScorerException 해당 기록원이 잠금하지 않은 경우
     */
    fun unlockScorer(scorerId: Long) {
        if (this.scorerId == null || this.scorerId != scorerId) {
            throw GameNotLockedByCurrentScorerException(id, scorerId)
        }
        this.scorerId = null
    }

    /**
     * 강제로 기록원 잠금을 해제합니다 (관리자용).
     */
    fun forceUnlockScorer() {
        this.scorerId = null
    }

    /**
     * 현재 기록원이 경기를 잠금하고 있는지 확인합니다.
     */
    fun isLockedByScorer(scorerId: Long): Boolean = this.scorerId == scorerId

    /**
     * 경기가 잠금 상태인지 확인합니다.
     */
    val isLocked: Boolean
        get() = scorerId != null

    /**
     * 다음 이닝으로 진행합니다.
     *
     * 연장전 타이브레이크 활성화 시 초(원정팀 공격) 시작마다 무사 1,2루 상태를 자동 설정합니다.
     * 최대 연장 이닝 도달 시 무승부로 경기를 종료합니다.
     *
     * @param tiebreakerFirstRunnerId  타이브레이크 1루 배치 주자 GamePlayer ID
     * @param tiebreakerSecondRunnerId 타이브레이크 2루 배치 주자 GamePlayer ID
     * @param gameTeams 무승부 자동 처리용 GameTeam 목록 (최대 연장 이닝 도달 시 DRAW 설정)
     * @return [TiebreakerResult] 이닝 전환 처리 결과
     */
    fun nextHalfInning(
        tiebreakerFirstRunnerId: Long? = null,
        tiebreakerSecondRunnerId: Long? = null,
        gameTeams: List<GameTeam> = emptyList(),
    ): TiebreakerResult {
        require(status.isOngoing()) { "진행 중인 경기만 이닝을 진행할 수 있습니다." }

        val rules = competition.gameRules

        // 말(홈팀 공격) 종료 후 다음 이닝으로 넘어가기 전에 최대 연장 이닝 제한 확인
        if (!isTopInning) {
            val maxExtra = rules.maxExtraInnings
            if (maxExtra != null && currentInning >= totalInnings + maxExtra) {
                status = GameStatus.FINISHED
                endedAt = LocalDateTime.now()
                gameTeams.forEach { it.updateResult(GameResult.DRAW) }
                return TiebreakerResult.DRAW_BY_INNINGS_LIMIT
            }
        }

        if (isTopInning) {
            isTopInning = false
        } else {
            currentInning++
            isTopInning = true
        }
        gameState.resetForNewInning()

        // 연장전 타이브레이크: 초(원정팀 공격) 시작 시에만 적용
        if (rules.tiebreakerEnabled && isTopInning && currentInning > totalInnings) {
            gameState.setupTiebreaker(
                firstRunnerId = tiebreakerFirstRunnerId,
                secondRunnerId = tiebreakerSecondRunnerId,
            )
            return TiebreakerResult.TIEBREAKER_APPLIED
        }

        return TiebreakerResult.NORMAL
    }

    /**
     * 투수 교체 가능 여부를 검증합니다 (한 타자 최소 대면 규칙).
     *
     * 사회인 야구 유연성을 위해 규칙 위반 시 예외를 던지지 않고 경고를 반환합니다.
     * - 이닝이 종료된 경우(아웃 3개) 교체는 자유롭습니다.
     * - 현재 투수의 대면 타자 수([battersFaced])가 [minBattersFaced] 미만이면 경고를 반환합니다.
     *
     * @param currentPitcherBattersFaced 현재 투수의 이번 경기 대면 타자 수
     * @param minBattersFaced 최소 대면 타자 수 (기본값 1)
     * @return [PitcherChangeWarning] 교체 가능 시 null, 경고 시 해당 경고 객체
     */
    fun checkPitcherChangeAllowed(
        currentPitcherBattersFaced: Int,
        minBattersFaced: Int = DEFAULT_MIN_BATTERS_FACED,
    ): PitcherChangeWarning? {
        require(status.isOngoing()) { "진행 중인 경기에서만 투수 교체를 확인할 수 있습니다." }
        require(minBattersFaced >= 1) { "최소 대면 타자 수는 1 이상이어야 합니다." }

        // 이닝 종료 후(아웃 3개) 교체는 항상 허용
        if (gameState.outs >= 3) return null

        return if (currentPitcherBattersFaced < minBattersFaced) {
            PitcherChangeWarning(
                currentBattersFaced = currentPitcherBattersFaced,
                minBattersFaced = minBattersFaced,
            )
        } else {
            null
        }
    }

    /**
     * 경기를 정상 종료합니다.
     *
     * 양 팀의 점수를 비교하여 WIN/LOSS/DRAW 결과를 자동으로 설정합니다.
     *
     * @param gameTeams 해당 경기에 참여하는 GameTeam 목록 (정확히 2개)
     */
    fun finish(gameTeams: List<GameTeam>) {
        require(status.isOngoing()) { "진행 중인 경기만 종료할 수 있습니다." }
        status = GameStatus.FINISHED
        endedAt = LocalDateTime.now()
        determineResults(gameTeams)
    }

    /**
     * 콜드게임으로 종료합니다.
     *
     * 최소 이닝 요건을 충족해야 콜드게임 선언이 가능합니다.
     * - 원칙: 최소 [minimumInning]회 이상 진행 후 선언 가능
     * - 예외: [minimumInning]회 초(top)까지 완료되고 홈팀이 리드 중이면
     *         [minimumInning - 1].5이닝에서도 선언 가능 (홈팀 승리 확정)
     *
     * 양 팀의 점수를 비교하여 WIN/LOSS/DRAW 결과를 자동으로 설정합니다.
     *
     * @param minimumInning 콜드게임 최소 요건 이닝 (기본값: 5이닝)
     * @param isHomeTeamLeading 홈팀이 리드 중인지 여부 (기본값: false)
     * @param reason 콜드게임 사유 (선택)
     * @param gameTeams 해당 경기에 참여하는 GameTeam 목록 (정확히 2개)
     */
    fun callGame(
        minimumInning: Int = DEFAULT_COLD_GAME_MINIMUM_INNING,
        isHomeTeamLeading: Boolean = false,
        reason: String? = null,
        gameTeams: List<GameTeam> = emptyList(),
    ) {
        require(status.isOngoing()) { "진행 중인 경기만 콜드게임 처리할 수 있습니다." }
        require(minimumInning >= 1) { "최소 이닝은 1 이상이어야 합니다." }

        val meetsMinimumRequirement =
            currentInning > minimumInning ||
                (currentInning == minimumInning) ||
                (currentInning == minimumInning - 1 && !isTopInning && isHomeTeamLeading)

        require(meetsMinimumRequirement) {
            "콜드게임은 최소 ${minimumInning}이닝(홈팀 리드 시 ${minimumInning - 1}.5이닝) 이후에만 선언 가능합니다. " +
                "현재: ${currentInning}회${if (isTopInning) "초" else "말"}"
        }

        status = GameStatus.CALLED
        endedAt = LocalDateTime.now()
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "콜드게임 사유: $reason"
        }
        if (gameTeams.isNotEmpty()) {
            determineResults(gameTeams)
        }
    }

    /**
     * Mercy Rule(콜드게임) 조건이 충족되었는지 확인합니다.
     *
     * 조건:
     * 1. 경기가 진행 중이어야 합니다.
     * 2. 현재 이닝이 [mercyMinimumInning] 이상이어야 합니다.
     * 3. 두 팀 간 점수 차이가 [mercyRunDifference] 이상이어야 합니다.
     *
     * 이 메서드는 조건 판별만 수행하며, 실제 콜드게임 처리는
     * 기록원이 [callGame]을 명시적으로 호출해야 합니다.
     *
     * @param homeScore 홈팀 현재 점수
     * @param awayScore 원정팀 현재 점수
     * @param mercyRunDifference 머시룰 점수 차이 기준 (기본값: 10점)
     * @param mercyMinimumInning 머시룰 적용 최소 이닝 (기본값: 5이닝)
     * @return Mercy Rule 조건 충족 여부
     */
    fun checkMercyRuleCondition(
        homeScore: Int,
        awayScore: Int,
        mercyRunDifference: Int = DEFAULT_MERCY_RUN_DIFFERENCE,
        mercyMinimumInning: Int = DEFAULT_MERCY_MINIMUM_INNING,
    ): Boolean {
        require(mercyRunDifference > 0) { "머시룰 점수 차이 기준은 1 이상이어야 합니다." }
        require(mercyMinimumInning >= 1) { "머시룰 최소 이닝은 1 이상이어야 합니다." }

        if (!status.isOngoing()) return false
        if (currentInning < mercyMinimumInning) return false
        return kotlin.math.abs(homeScore - awayScore) >= mercyRunDifference
    }

    /**
     * 시간 제한 상태를 확인합니다 (M-5: 시간 제한 경고/알림).
     *
     * 경기 규칙에 timeLimitMinutes가 설정된 경우, 경기 시작 시각 기준
     * 현재 경과 시간을 계산하여 경고/도달 상태를 반환합니다.
     *
     * @param now 현재 시각
     * @param warningThresholdMinutes 경고 임박 기준 (분, 기본 10분 전)
     * @return 시간 제한 상태 (null이면 제한 없음 또는 정상 범위)
     */
    fun checkTimeLimitStatus(
        now: java.time.LocalDateTime,
        warningThresholdMinutes: Int = DEFAULT_TIME_LIMIT_WARNING_THRESHOLD,
    ): TimeLimitStatus? {
        if (!status.isOngoing()) return null
        val limit = competition.gameRules.timeLimitMinutes ?: return null
        val started = startedAt ?: return null

        val elapsedMinutes =
            java.time.Duration.between(started, now).toMinutes()

        return when {
            elapsedMinutes >= limit -> TimeLimitStatus.LIMIT_REACHED
            elapsedMinutes >= limit - warningThresholdMinutes -> TimeLimitStatus.APPROACHING_LIMIT
            else -> null
        }
    }

    /**
     * 경기를 중단합니다.
     *
     * 진행 중인 경기를 우천/조명 고장 등의 사유로 중단합니다.
     * 현재 이닝, 아웃카운트, 주자 상태 등 GameState가 그대로 보존됩니다.
     *
     * @param reason 중단 사유 (선택)
     */
    fun suspend(reason: String? = null) {
        require(status.isOngoing()) { "진행 중인 경기만 중단할 수 있습니다." }
        status = GameStatus.SUSPENDED
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "중단 사유: $reason"
        }
    }

    /**
     * 중단된 경기를 재개합니다.
     *
     * 중단 시점의 이닝, 아웃카운트, 주자 상태부터 이어서 진행합니다.
     */
    fun resume() {
        require(status.isSuspended()) { "중단된 경기만 재개할 수 있습니다. 현재 상태: ${status.displayName}" }
        status = GameStatus.IN_PROGRESS
    }

    /**
     * 경기를 취소합니다.
     */
    fun cancel(reason: String? = null) {
        require(status == GameStatus.SCHEDULED || status == GameStatus.POSTPONED || status == GameStatus.SUSPENDED) {
            "예정, 연기, 또는 중단 상태의 경기만 취소할 수 있습니다."
        }
        status = GameStatus.CANCELLED
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "취소 사유: $reason"
        }
    }

    /**
     * 경기를 연기합니다.
     */
    fun postpone(
        newScheduledAt: LocalDateTime,
        reason: String? = null,
    ) {
        require(status == GameStatus.SCHEDULED) { "예정 상태의 경기만 연기할 수 있습니다." }
        status = GameStatus.POSTPONED
        scheduledAt = newScheduledAt
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "연기 사유: $reason"
        }
    }

    /**
     * 몰수패 처리합니다.
     *
     * 승리팀에 7점, 패배팀에 0점을 자동 반영하고,
     * 각 GameTeam의 결과(WIN/LOSS)를 설정합니다.
     *
     * @param winnerTeamId 몰수승 팀 ID
     * @param reason 몰수 사유
     * @param gameTeams 해당 경기에 참여하는 GameTeam 목록 (정확히 2개)
     */
    fun forfeit(
        winnerTeamId: Long,
        reason: String,
        gameTeams: List<GameTeam>,
    ) {
        require(status == GameStatus.SCHEDULED || status == GameStatus.POSTPONED || status.isOngoing()) {
            "예정, 연기, 또는 진행 중인 경기만 몰수 처리할 수 있습니다."
        }
        require(gameTeams.size == 2) {
            "몰수 처리를 위해서는 정확히 2개의 GameTeam이 필요합니다."
        }
        require(gameTeams.any { it.team.id == winnerTeamId }) {
            "승리팀 ID($winnerTeamId)가 해당 경기에 참여하는 팀이 아닙니다."
        }

        status = GameStatus.FORFEITED
        endedAt = LocalDateTime.now()
        forfeitReason = reason
        note = (note?.let { "$it\n" } ?: "") + "몰수 사유: $reason"

        // 승리팀에 대회 규칙의 몰수승 점수 반영
        val forfeitWinScore = competition.gameRules.forfeitScore
        gameTeams.forEach { gameTeam ->
            if (gameTeam.team.id == winnerTeamId) {
                gameTeam.updateScore(totalScore = forfeitWinScore, totalHits = 0, totalErrors = 0)
                gameTeam.updateResult(GameResult.WIN)
            } else {
                gameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
                gameTeam.updateResult(GameResult.LOSS)
            }
        }
    }

    /**
     * 양 팀의 점수를 비교하여 GameTeam의 result를 설정합니다.
     *
     * - 점수가 높은 팀: WIN, 낮은 팀: LOSS
     * - 동점: 양 팀 모두 DRAW
     *
     * @param gameTeams 해당 경기에 참여하는 GameTeam 목록 (정확히 2개)
     */
    private fun determineResults(gameTeams: List<GameTeam>) {
        require(gameTeams.size == 2) {
            "결과 판정을 위해서는 정확히 2개의 GameTeam이 필요합니다."
        }

        val team1 = gameTeams[0]
        val team2 = gameTeams[1]

        when {
            team1.totalScore > team2.totalScore -> {
                team1.updateResult(GameResult.WIN)
                team2.updateResult(GameResult.LOSS)
            }
            team1.totalScore < team2.totalScore -> {
                team1.updateResult(GameResult.LOSS)
                team2.updateResult(GameResult.WIN)
            }
            else -> {
                team1.updateResult(GameResult.DRAW)
                team2.updateResult(GameResult.DRAW)
            }
        }
    }

    companion object {
        /** 몰수승 기본 점수 (GameRules.forfeitScore 기본값과 일치) */
        const val DEFAULT_FORFEIT_WIN_SCORE = 7

        /** 콜드게임 기본 최소 이닝 */
        const val DEFAULT_COLD_GAME_MINIMUM_INNING = 5

        /** Mercy Rule 기본 점수 차이 */
        const val DEFAULT_MERCY_RUN_DIFFERENCE = 10

        /** Mercy Rule 기본 최소 이닝 */
        const val DEFAULT_MERCY_MINIMUM_INNING = 5

        /** 투수 교체 시 최소 대면 타자 수 기본값 */
        const val DEFAULT_MIN_BATTERS_FACED = 1

        /** 시간 제한 경고 임박 기준 (분) */
        const val DEFAULT_TIME_LIMIT_WARNING_THRESHOLD = 10

        /** L-4: 더블헤더 이닝 축소 기본값 (기본 이닝 - 2) */
        const val DEFAULT_DOUBLEHEADER_INNING_REDUCTION = 2

        /** L-11: SUSPENDED 경기 자동 타임아웃 기본값 (48시간) */
        const val DEFAULT_SUSPENDED_TIMEOUT_HOURS = 48L

        /**
         * 프로덕션 팩토리 메서드.
         *
         * 홈팀/원정팀 [GameTeam] 2개를 자동 생성하며,
         * [competition]의 [GameRules.defaultInnings]를 totalInnings로 설정합니다.
         */
        fun create(
            competition: Competition,
            homeTeam: Team,
            awayTeam: Team,
            scheduledAt: LocalDateTime,
            location: String? = null,
            fieldName: String? = null,
            gameNumber: Int? = null,
            isDoubleheader: Boolean = false,
        ): Game {
            require(homeTeam.id != awayTeam.id) { "홈팀과 원정팀은 같을 수 없습니다." }
            if (isDoubleheader) {
                require(gameNumber != null && gameNumber in 1..2) {
                    "더블헤더 경기는 gameNumber가 1 또는 2여야 합니다."
                }
            }

            // L-4: 더블헤더 시 이닝 자동 축소
            val effectiveInnings =
                if (isDoubleheader) {
                    maxOf(3, competition.gameRules.defaultInnings - DEFAULT_DOUBLEHEADER_INNING_REDUCTION)
                } else {
                    competition.gameRules.defaultInnings
                }

            val game =
                Game(
                    competition = competition,
                    scheduledAt = scheduledAt,
                    location = location,
                    fieldName = fieldName,
                    gameNumber = gameNumber,
                    isDoubleheader = isDoubleheader,
                    totalInnings = effectiveInnings,
                )
            game._gameTeams.add(GameTeam(game = game, team = homeTeam, homeAway = HomeAway.HOME))
            game._gameTeams.add(GameTeam(game = game, team = awayTeam, homeAway = HomeAway.AWAY))
            return game
        }

        /**
         * 테스트 전용 팩토리 메서드.
         *
         * 특정 상태(status, currentInning 등)의 Game을 생성할 때 사용합니다.
         * 프로덕션 코드에서는 반드시 [create]를 사용하세요.
         */
        @Suppress("LongParameterList")
        fun createForTest(
            competition: Competition,
            homeTeam: Team,
            awayTeam: Team,
            scheduledAt: LocalDateTime = LocalDateTime.now(),
            location: String? = null,
            fieldName: String? = null,
            gameNumber: Int? = null,
            isDoubleheader: Boolean = false,
            status: GameStatus = GameStatus.SCHEDULED,
            currentInning: Int = 0,
            isTopInning: Boolean = true,
            totalInnings: Int = competition.gameRules.defaultInnings,
            startedAt: LocalDateTime? = null,
            endedAt: LocalDateTime? = null,
            note: String? = null,
            forfeitReason: String? = null,
            gameState: GameState = GameState(),
            scorerId: Long? = null,
            id: Long = 0L,
        ): Game {
            val game =
                Game(
                    competition = competition,
                    scheduledAt = scheduledAt,
                    location = location,
                    fieldName = fieldName,
                    gameNumber = gameNumber,
                    isDoubleheader = isDoubleheader,
                    status = status,
                    currentInning = currentInning,
                    isTopInning = isTopInning,
                    totalInnings = totalInnings,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    note = note,
                    forfeitReason = forfeitReason,
                    gameState = gameState,
                    scorerId = scorerId,
                    id = id,
                )
            game._gameTeams.add(GameTeam(game = game, team = homeTeam, homeAway = HomeAway.HOME, id = id * 100 + 1))
            game._gameTeams.add(GameTeam(game = game, team = awayTeam, homeAway = HomeAway.AWAY, id = id * 100 + 2))
            return game
        }
    }

    /**
     * 경기 일정을 변경합니다.
     */
    fun reschedule(newScheduledAt: LocalDateTime) {
        require(status == GameStatus.SCHEDULED || status == GameStatus.POSTPONED) {
            "예정 또는 연기 상태의 경기만 일정을 변경할 수 있습니다."
        }
        if (status == GameStatus.POSTPONED) {
            status = GameStatus.SCHEDULED
        }
        scheduledAt = newScheduledAt
    }

    /**
     * Undo가 가능한 상태인지 확인합니다.
     */
    fun canUndo(): Boolean = status == GameStatus.IN_PROGRESS

    /**
     * 이전 이닝 상태로 복원합니다 (Undo 시 이닝/아웃 카운트 롤백용).
     */
    fun restoreInningState(
        inning: Int,
        isTop: Boolean,
        outs: Int,
    ) {
        require(status.isOngoing()) { "진행 중인 경기만 상태를 복원할 수 있습니다." }
        this.currentInning = inning
        this.isTopInning = isTop
        gameState.restoreOuts(outs)
    }

    /**
     * 타순을 되돌립니다 (Undo 시 타순 롤백용).
     */
    fun revertBatter() {
        require(status.isOngoing()) { "진행 중인 경기만 타순을 되돌릴 수 있습니다." }
        gameState.revertBatter(isHomeTeam = !isTopInning)
    }

    /**
     * 현재 이닝 표시를 반환합니다 (예: "5회초", "7회말").
     */
    val currentInningDisplay: String
        get() = if (currentInning == 0) "경기 전" else "${currentInning}회${if (isTopInning) "초" else "말"}"

    /**
     * 경기가 연장전인지 확인합니다.
     */
    val isExtraInning: Boolean
        get() = currentInning > totalInnings

    /**
     * 더블헤더 표시를 반환합니다 (예: "제1경기", "제2경기").
     * 더블헤더가 아닌 경우 null을 반환합니다.
     */
    val doubleheaderDisplay: String?
        get() =
            if (isDoubleheader && gameNumber != null) {
                "제${gameNumber}경기"
            } else {
                null
            }

    /**
     * L-11: 중단된 경기가 타임아웃 되었는지 확인합니다.
     *
     * @param timeoutHours 타임아웃 시간 (기본 48시간)
     * @param now 현재 시각 (Instant)
     * @return 타임아웃 여부
     */
    fun isSuspendedTimeout(
        timeoutHours: Long = DEFAULT_SUSPENDED_TIMEOUT_HOURS,
        now: java.time.Instant = java.time.Instant.now(),
    ): Boolean {
        if (status != GameStatus.SUSPENDED) return false
        return Duration.between(updatedAt, now).toHours() >= timeoutHours
    }

    /**
     * L-11: 타임아웃으로 중단된 경기를 취소 처리합니다.
     */
    fun cancelByTimeout() {
        require(status == GameStatus.SUSPENDED) {
            "중단 상태의 경기만 타임아웃으로 취소할 수 있습니다."
        }
        status = GameStatus.CANCELLED
        note = (note?.let { "$it\n" } ?: "") + "타임아웃으로 자동 취소됨"
    }

    /**
     * 아웃을 기록합니다.
     * @return 3아웃으로 이닝이 종료되었는지 여부
     */
    fun recordOut(): Boolean {
        require(status.isOngoing()) { "진행 중인 경기만 아웃을 기록할 수 있습니다." }
        return gameState.recordOut()
    }

    /**
     * 다음 타자로 타순을 진행합니다.
     */
    fun advanceBatter() {
        require(status.isOngoing()) { "진행 중인 경기만 타순을 진행할 수 있습니다." }
        gameState.advanceBatter(isHomeTeam = !isTopInning)
    }

    /**
     * 주자를 설정합니다.
     */
    fun setRunner(
        base: Base,
        playerId: Long?,
    ) {
        require(status.isOngoing()) { "진행 중인 경기만 주자를 설정할 수 있습니다." }
        gameState.setRunner(base, playerId)
    }

    /**
     * 모든 베이스를 클리어합니다.
     */
    fun clearBases() {
        require(status.isOngoing()) { "진행 중인 경기만 베이스를 클리어할 수 있습니다." }
        gameState.clearBases()
    }

    /**
     * 볼카운트를 리셋합니다.
     */
    fun resetCount() {
        require(status.isOngoing()) { "진행 중인 경기만 볼카운트를 리셋할 수 있습니다." }
        gameState.resetCount()
    }

    /**
     * 볼을 추가합니다.
     * @return 4볼로 볼넷 여부
     */
    fun addBall(): Boolean {
        require(status.isOngoing()) { "진행 중인 경기만 볼을 추가할 수 있습니다." }
        return gameState.addBall()
    }

    /**
     * 스트라이크를 추가합니다.
     * @return 3스트라이크로 삼진 여부
     */
    fun addStrike(): Boolean {
        require(status.isOngoing()) { "진행 중인 경기만 스트라이크를 추가할 수 있습니다." }
        return gameState.addStrike()
    }
}
