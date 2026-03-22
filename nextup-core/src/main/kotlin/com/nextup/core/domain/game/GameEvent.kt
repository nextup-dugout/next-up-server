package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * 경기 이벤트 엔티티
 *
 * 경기 중 발생하는 모든 이벤트를 기록합니다.
 */
@Entity
@Table(
    name = "game_events",
    indexes = [
        Index(name = "idx_game_events_game", columnList = "game_id"),
        Index(name = "idx_game_events_game_inning", columnList = "game_id, inning, is_top_inning"),
        Index(name = "idx_game_events_timestamp", columnList = "event_timestamp"),
    ],
)
class GameEvent(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    val game: Game,
    @Column(nullable = false)
    val inning: Int,
    @Column(name = "is_top_inning", nullable = false)
    val isTopInning: Boolean,
    @Column(name = "out_count_before", nullable = false)
    val outCountBefore: Int,
    @Column(name = "out_count_after", nullable = false)
    val outCountAfter: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    val eventType: GameEventType,
    @Column(nullable = false, length = 500)
    var description: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batter_id")
    val batter: GamePlayer? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitcher_id")
    val pitcher: GamePlayer? = null,
    @Column(name = "runners_before_json", columnDefinition = "TEXT")
    val runnersBeforeJson: String? = null,
    @Column(name = "runners_after_json", columnDefinition = "TEXT")
    val runnersAfterJson: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "plate_appearance_result", length = 30)
    var plateAppearanceResult: PlateAppearanceResult? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runner_player_id")
    val runnerPlayer: GamePlayer? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "from_base", length = 10)
    val fromBase: Base? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "to_base", length = 10)
    val toBase: Base? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "base_running_result", length = 30)
    val baseRunningResult: BaseRunningResult? = null,
    @Column(name = "runs_scored", nullable = false)
    val runsScored: Int = 0,
    @Column(nullable = false)
    val rbis: Int = 0,
    /**
     * 득점 주자 ID 목록 (D-15: 타점 경로 상세 기록)
     * 형식: "playerId1,playerId2,..." (CSV)
     * 홈런 타자 자신은 타자 ID로 포함됨
     */
    @Column(name = "scoring_runner_ids", columnDefinition = "TEXT")
    val scoringRunnerIds: String? = null,
    @Column(name = "event_timestamp", nullable = false)
    val eventTimestamp: Instant = Instant.now(),
    @Column(nullable = false)
    var undone: Boolean = false,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    fun markUndone() {
        undone = true
    }

    /**
     * 타석 결과와 설명을 정정합니다 (H6: 기록 정정 반영).
     *
     * @param newResult 정정된 타석 결과
     * @param newDescription 정정된 설명
     */
    fun correctResult(
        newResult: PlateAppearanceResult,
        newDescription: String,
    ) {
        this.plateAppearanceResult = newResult
        this.description = newDescription
    }

    /**
     * 득점 주자 ID 목록을 반환합니다 (D-15: 타점 경로).
     */
    fun getScoringRunnerIdList(): List<Long> = parseScoringRunnerIds(scoringRunnerIds)

    companion object {
        /**
         * 타석 결과 이벤트를 생성합니다.
         */
        fun createPlateAppearance(
            game: Game,
            batter: GamePlayer,
            pitcher: GamePlayer,
            result: PlateAppearanceResult,
            description: String,
            outCountBefore: Int,
            outCountAfter: Int,
            runnersBeforeJson: String?,
            runnersAfterJson: String?,
            runsScored: Int = 0,
            rbis: Int = 0,
            scoringRunnerIds: List<Long> = emptyList(),
        ): GameEvent =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = outCountBefore,
                outCountAfter = outCountAfter,
                eventType = GameEventType.PLATE_APPEARANCE,
                description = description,
                batter = batter,
                pitcher = pitcher,
                runnersBeforeJson = runnersBeforeJson,
                runnersAfterJson = runnersAfterJson,
                plateAppearanceResult = result,
                runsScored = runsScored,
                rbis = rbis,
                scoringRunnerIds = scoringRunnerIds.joinToString(",").ifEmpty { null },
            )

        /**
         * 득점 주자 ID 목록을 파싱합니다.
         */
        fun parseScoringRunnerIds(scoringRunnerIds: String?): List<Long> {
            if (scoringRunnerIds.isNullOrBlank()) return emptyList()
            return scoringRunnerIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        }

        /**
         * 이닝 전환 이벤트를 생성합니다.
         */
        fun createInningChange(
            game: Game,
            description: String,
        ): GameEvent =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = 3,
                outCountAfter = 0,
                eventType = GameEventType.INNING_CHANGE,
                description = description,
            )

        /**
         * 주루 플레이 이벤트를 생성합니다.
         */
        fun createBaseRunning(
            game: Game,
            runner: GamePlayer,
            fromBase: Base,
            toBase: Base,
            result: BaseRunningResult,
            description: String,
            outCountBefore: Int,
            outCountAfter: Int,
            runnersBeforeJson: String?,
            runnersAfterJson: String?,
        ): GameEvent =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = outCountBefore,
                outCountAfter = outCountAfter,
                eventType = GameEventType.BASE_RUNNING,
                description = description,
                runnerPlayer = runner,
                fromBase = fromBase,
                toBase = toBase,
                baseRunningResult = result,
                runnersBeforeJson = runnersBeforeJson,
                runnersAfterJson = runnersAfterJson,
            )

        /**
         * 경기 상태 변경 이벤트를 생성합니다.
         */
        fun createGameStatus(
            game: Game,
            description: String,
        ): GameEvent =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = game.gameState.outs,
                outCountAfter = game.gameState.outs,
                eventType = GameEventType.GAME_STATUS,
                description = description,
            )

        /**
         * 선수 교체 이벤트를 생성합니다.
         *
         * @param game 경기
         * @param incomingPlayer 교체 들어오는 선수
         * @param outgoingPlayer 교체 나가는 선수
         * @param description 교체 설명
         */
        fun createSubstitution(
            game: Game,
            incomingPlayer: GamePlayer,
            outgoingPlayer: GamePlayer,
            description: String,
        ): GameEvent =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = game.gameState.outs,
                outCountAfter = game.gameState.outs,
                eventType = GameEventType.SUBSTITUTION,
                description = description,
                batter = incomingPlayer,
                pitcher = outgoingPlayer,
            )

        /**
         * 포지션 변경 이벤트를 생성합니다.
         *
         * @param game 경기
         * @param player 포지션이 변경된 선수
         * @param fromPosition 변경 전 포지션
         * @param toPosition 변경 후 포지션
         * @param description 포지션 변경 설명
         */
        fun createPositionChange(
            game: Game,
            player: GamePlayer,
            fromPosition: com.nextup.core.domain.player.Position,
            toPosition: com.nextup.core.domain.player.Position,
            description: String,
        ): GameEvent =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = game.gameState.outs,
                outCountAfter = game.gameState.outs,
                eventType = GameEventType.POSITION_CHANGE,
                description = description,
                batter = player,
            )

        /**
         * 퇴장 이벤트를 생성합니다.
         *
         * @param game 경기
         * @param ejectedPlayer 퇴장 선수
         * @param description 퇴장 사유 (예: "폭력", "항의", "기타")
         */
        fun createEjection(
            game: Game,
            ejectedPlayer: GamePlayer,
            description: String,
        ): GameEvent =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = game.gameState.outs,
                outCountAfter = game.gameState.outs,
                eventType = GameEventType.EJECTION,
                description = description,
                batter = ejectedPlayer,
            )

        /**
         * 긴급 교체 이벤트를 생성합니다.
         *
         * 퇴장(부상/심판 퇴장 등)으로 인한 긴급 교체를 기록합니다.
         * 일반 교체(SUBSTITUTION)와 구분되며, 퇴장 사유를 포함합니다.
         *
         * @param game 경기
         * @param incomingPlayer 교체 들어오는 선수
         * @param outgoingPlayer 퇴장하는 선수
         * @param reason 퇴장 사유
         * @param description 긴급 교체 설명
         */
        fun createEmergencySubstitution(
            game: Game,
            incomingPlayer: GamePlayer,
            outgoingPlayer: GamePlayer,
            reason: EjectionReason,
            description: String,
        ): GameEvent =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = game.gameState.outs,
                outCountAfter = game.gameState.outs,
                eventType = GameEventType.EMERGENCY_SUBSTITUTION,
                description = description,
                batter = incomingPlayer,
                pitcher = outgoingPlayer,
            )
    }
}
