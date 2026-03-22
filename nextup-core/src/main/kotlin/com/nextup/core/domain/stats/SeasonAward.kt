package com.nextup.core.domain.stats

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * 시즌 타이틀(개인상) 엔티티
 *
 * 시즌 종료 시 각 부문별 최고 성적을 거둔 선수에게 부여되는 타이틀을 저장합니다.
 * 한 시즌에 동일 타이틀은 중복 부여되지 않습니다 (공동 수상 시 여러 레코드 생성).
 */
@Entity
@Table(
    name = "season_awards",
    indexes = [
        Index(name = "idx_season_awards_player", columnList = "player_id"),
        Index(name = "idx_season_awards_year", columnList = "year"),
        Index(name = "idx_season_awards_title", columnList = "title"),
        Index(name = "idx_season_awards_year_title", columnList = "year, title"),
        Index(name = "idx_season_awards_competition", columnList = "competition_id"),
    ],
)
class SeasonAward private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Column(nullable = false)
    val year: Int,
    /** 대회 ID (대회별 타이틀 조회에 사용) */
    @Column(name = "competition_id")
    val competitionId: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val title: SeasonAwardTitle,
    /**
     * 수상 기준 값 (타율, ERA, 홈런 수 등)
     * 표시 및 참조용으로 저장합니다.
     */
    @Column(name = "stat_value", precision = 10, scale = 3)
    val statValue: BigDecimal? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        /**
         * 시즌 타이틀을 생성합니다.
         *
         * @param player 수상 선수
         * @param year 시즌 연도
         * @param competitionId 대회 ID (선택)
         * @param title 타이틀 종류
         * @param statValue 수상 기준 값
         */
        fun create(
            player: Player,
            year: Int,
            title: SeasonAwardTitle,
            statValue: BigDecimal? = null,
            competitionId: Long? = null,
        ): SeasonAward {
            require(year > 0) { "연도는 양수여야 합니다." }
            return SeasonAward(
                player = player,
                year = year,
                competitionId = competitionId,
                title = title,
                statValue = statValue,
            )
        }
    }
}
