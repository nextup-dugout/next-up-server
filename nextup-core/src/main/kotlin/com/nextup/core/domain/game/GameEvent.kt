package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * 경기 이벤트 엔티티
 *
 * 경기 중 발생하는 모든 이벤트를 기록합니다.
 * 타석 결과, 주루 이벤트, 선수 교체, 이닝 전환 등을 포함합니다.
 */
@Entity
@Table(
    name = "game_events",
    indexes = [
        Index(name = "idx_game_events_game", columnList = "game_id"),
        Index(name = "idx_game_events_game_order", columnList = "game_id, event_order"),
        Index(name = "idx_game_events_game_inning", columnList = "game_id, inning, is_top_inning"),
        Index(name = "idx_game_events_type", columnList = "event_type"),
        Index(name = "idx_game_events_batter", columnList = "batter_id"),
        Index(name = "idx_game_events_pitcher", columnList = "pitcher_id")
    ]
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "involved_runner_id")
    val involvedRunner: GamePlayer? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substituted_in_id")
    val substitutedIn: GamePlayer? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substituted_out_id")
    val substitutedOut: GamePlayer? = null,

    @Column(name = "runners_before", columnDefinition = "TEXT")
    val runnersBefore: String? = null,

    @Column(name = "runners_after", columnDefinition = "TEXT")
    val runnersAfter: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "plate_appearance_result", length = 30)
    val plateAppearanceResult: PlateAppearanceResult? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "base_running_event", length = 30)
    val baseRunningEvent: BaseRunningEvent? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "substitution_type", length = 30)
    val substitutionType: SubstitutionType? = null,

    @Column(name = "runs_scored", nullable = false)
    val runsScored: Int = 0,

    @Column(nullable = false)
    val rbis: Int = 0,

    @Column(name = "is_earned_run", nullable = false)
    val isEarnedRun: Boolean = true,

    @Column(name = "event_order", nullable = false)
    val eventOrder: Int,

    @Column(name = "event_timestamp", nullable = false)
    val eventTimestamp: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity() {

    init {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
        require(outCountBefore in 0..3) { "아웃 카운트(before)는 0~3 사이여야 합니다." }
        require(outCountAfter in 0..3) { "아웃 카운트(after)는 0~3 사이여야 합니다." }
        require(runsScored >= 0) { "득점은 0 이상이어야 합니다." }
        require(rbis >= 0) { "타점은 0 이상이어야 합니다." }
        require(eventOrder >= 1) { "이벤트 순서는 1 이상이어야 합니다." }

        if (eventType == GameEventType.PLATE_APPEARANCE) {
            requireNotNull(batter) { "타석 결과에는 타자가 필수입니다." }
            requireNotNull(pitcher) { "타석 결과에는 투수가 필수입니다." }
            requireNotNull(plateAppearanceResult) { "타석 결과에는 결과 유형이 필수입니다." }
        }

        if (eventType == GameEventType.BASE_RUNNING) {
            requireNotNull(baseRunningEvent) { "주루 이벤트에는 이벤트 유형이 필수입니다." }
        }

        if (eventType == GameEventType.SUBSTITUTION) {
            requireNotNull(substitutionType) { "선수 교체에는 교체 유형이 필수입니다." }
            requireNotNull(substitutedIn) { "선수 교체에는 교체 투입 선수가 필수입니다." }
        }
    }

    /**
     * 이닝 표시 (예: "5회초", "7회말")
     */
    val inningDisplay: String
        get() = "${inning}회${if (isTopInning) "초" else "말"}"

    /**
     * 아웃 추가 수
     */
    val outsAdded: Int
        get() = outCountAfter - outCountBefore

    /**
     * 이닝 종료 이벤트인지 확인
     */
    val isInningEndingEvent: Boolean
        get() = outCountAfter >= 3

    /**
     * 득점이 발생한 이벤트인지 확인
     */
    val isScoringEvent: Boolean
        get() = runsScored > 0

    companion object {
        /**
         * 타석 결과 이벤트를 생성합니다.
         */
        fun createPlateAppearance(
            game: Game,
            inning: Int,
            isTopInning: Boolean,
            outCountBefore: Int,
            outCountAfter: Int,
            batter: GamePlayer,
            pitcher: GamePlayer,
            result: PlateAppearanceResult,
            description: String,
            runsScored: Int = 0,
            rbis: Int = 0,
            isEarnedRun: Boolean = true,
            runnersBefore: String? = null,
            runnersAfter: String? = null,
            eventOrder: Int
        ): GameEvent = GameEvent(
            game = game,
            inning = inning,
            isTopInning = isTopInning,
            outCountBefore = outCountBefore,
            outCountAfter = outCountAfter,
            eventType = GameEventType.PLATE_APPEARANCE,
            description = description,
            batter = batter,
            pitcher = pitcher,
            plateAppearanceResult = result,
            runsScored = runsScored,
            rbis = rbis,
            isEarnedRun = isEarnedRun,
            runnersBefore = runnersBefore,
            runnersAfter = runnersAfter,
            eventOrder = eventOrder
        )

        /**
         * 주루 이벤트를 생성합니다.
         */
        fun createBaseRunning(
            game: Game,
            inning: Int,
            isTopInning: Boolean,
            outCountBefore: Int,
            outCountAfter: Int,
            runner: GamePlayer,
            pitcher: GamePlayer,
            event: BaseRunningEvent,
            description: String,
            runsScored: Int = 0,
            isEarnedRun: Boolean = true,
            runnersBefore: String? = null,
            runnersAfter: String? = null,
            eventOrder: Int
        ): GameEvent = GameEvent(
            game = game,
            inning = inning,
            isTopInning = isTopInning,
            outCountBefore = outCountBefore,
            outCountAfter = outCountAfter,
            eventType = GameEventType.BASE_RUNNING,
            description = description,
            pitcher = pitcher,
            involvedRunner = runner,
            baseRunningEvent = event,
            runsScored = runsScored,
            isEarnedRun = isEarnedRun,
            runnersBefore = runnersBefore,
            runnersAfter = runnersAfter,
            eventOrder = eventOrder
        )

        /**
         * 선수 교체 이벤트를 생성합니다.
         */
        fun createSubstitution(
            game: Game,
            inning: Int,
            isTopInning: Boolean,
            outCount: Int,
            substitutionType: SubstitutionType,
            playerIn: GamePlayer,
            playerOut: GamePlayer?,
            description: String,
            eventOrder: Int
        ): GameEvent = GameEvent(
            game = game,
            inning = inning,
            isTopInning = isTopInning,
            outCountBefore = outCount,
            outCountAfter = outCount,
            eventType = GameEventType.SUBSTITUTION,
            description = description,
            substitutedIn = playerIn,
            substitutedOut = playerOut,
            substitutionType = substitutionType,
            eventOrder = eventOrder
        )

        /**
         * 이닝 전환 이벤트를 생성합니다.
         */
        fun createHalfInningChange(
            game: Game,
            inning: Int,
            isTopInning: Boolean,
            description: String,
            eventOrder: Int
        ): GameEvent = GameEvent(
            game = game,
            inning = inning,
            isTopInning = isTopInning,
            outCountBefore = 3,
            outCountAfter = 0,
            eventType = GameEventType.HALF_INNING_CHANGE,
            description = description,
            eventOrder = eventOrder
        )

        /**
         * 경기 시작 이벤트를 생성합니다.
         */
        fun createGameStart(
            game: Game,
            description: String = "경기 시작",
            eventOrder: Int = 1
        ): GameEvent = GameEvent(
            game = game,
            inning = 1,
            isTopInning = true,
            outCountBefore = 0,
            outCountAfter = 0,
            eventType = GameEventType.GAME_START,
            description = description,
            eventOrder = eventOrder
        )

        /**
         * 경기 종료 이벤트를 생성합니다.
         */
        fun createGameEnd(
            game: Game,
            inning: Int,
            isTopInning: Boolean,
            outCount: Int,
            description: String = "경기 종료",
            eventOrder: Int
        ): GameEvent = GameEvent(
            game = game,
            inning = inning,
            isTopInning = isTopInning,
            outCountBefore = outCount,
            outCountAfter = outCount,
            eventType = GameEventType.GAME_END,
            description = description,
            eventOrder = eventOrder
        )
    }
}
