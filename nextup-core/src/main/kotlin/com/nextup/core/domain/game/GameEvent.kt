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
    val description: String,
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
    val plateAppearanceResult: PlateAppearanceResult? = null,
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
    }
}
