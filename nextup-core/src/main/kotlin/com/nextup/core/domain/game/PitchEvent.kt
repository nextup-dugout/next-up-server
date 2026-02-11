package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*

/**
 * 투구 이벤트 엔티티
 *
 * 경기 중 발생하는 개별 투구를 기록합니다.
 * 각 투구마다 투구 번호, 결과, 볼카운트를 추적합니다.
 */
@Entity
@Table(
    name = "pitch_events",
    indexes = [
        Index(name = "idx_pitch_events_game", columnList = "game_id"),
        Index(name = "idx_pitch_events_game_inning", columnList = "game_id, inning, is_top_inning"),
        Index(name = "idx_pitch_events_pitcher", columnList = "pitcher_id"),
        Index(name = "idx_pitch_events_batter", columnList = "batter_id"),
        Index(name = "idx_pitch_events_game_pitch_number", columnList = "game_id, pitch_number"),
    ],
)
class PitchEvent(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    val game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitcher_id", nullable = false)
    val pitcher: GamePlayer,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batter_id", nullable = false)
    val batter: GamePlayer,
    @Column(nullable = false)
    val inning: Int,
    @Column(name = "is_top_inning", nullable = false)
    val isTopInning: Boolean,
    @Column(name = "pitch_number", nullable = false)
    val pitchNumber: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val result: PitchResult,
    @Column(name = "ball_count", nullable = false)
    val ballCount: Int,
    @Column(name = "strike_count", nullable = false)
    val strikeCount: Int,
    @Column(length = 500)
    val description: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    init {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다: $inning" }
        require(pitchNumber >= 1) { "투구 번호는 1 이상이어야 합니다: $pitchNumber" }
        require(ballCount in 0..4) { "볼카운트는 0-4 사이여야 합니다: $ballCount" }
        require(strikeCount in 0..3) { "스트라이크 카운트는 0-3 사이여야 합니다: $strikeCount" }
    }

    /**
     * 현재 볼카운트를 문자열로 반환합니다 (예: "2B-1S").
     */
    val countDisplay: String
        get() = "${ballCount}B-${strikeCount}S"

    /**
     * 풀카운트인지 확인합니다 (3-2 카운트).
     */
    val isFullCount: Boolean
        get() = ballCount == 3 && strikeCount == 2

    /**
     * 타자에게 유리한 카운트인지 확인합니다 (볼이 스트라이크보다 많음).
     */
    val isHitterCount: Boolean
        get() = ballCount > strikeCount

    /**
     * 투수에게 유리한 카운트인지 확인합니다 (스트라이크가 볼보다 많음).
     */
    val isPitcherCount: Boolean
        get() = strikeCount > ballCount

    companion object {
        /**
         * 투구 이벤트를 생성합니다.
         */
        fun create(
            game: Game,
            pitcher: GamePlayer,
            batter: GamePlayer,
            pitchNumber: Int,
            result: PitchResult,
            ballCount: Int,
            strikeCount: Int,
            description: String? = null,
        ): PitchEvent {
            require(pitcher.isPitcher) { "투수 포지션의 선수만 투구할 수 있습니다." }
            require(pitcher.gameTeam.game.id == game.id) { "투수는 해당 경기의 선수여야 합니다." }
            require(batter.gameTeam.game.id == game.id) { "타자는 해당 경기의 선수여야 합니다." }
            require(pitcher.gameTeam.id != batter.gameTeam.id) { "투수와 타자는 서로 다른 팀이어야 합니다." }

            return PitchEvent(
                game = game,
                pitcher = pitcher,
                batter = batter,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                pitchNumber = pitchNumber,
                result = result,
                ballCount = ballCount,
                strikeCount = strikeCount,
                description = description,
            )
        }
    }
}
