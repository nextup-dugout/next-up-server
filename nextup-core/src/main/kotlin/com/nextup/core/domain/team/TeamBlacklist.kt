package com.nextup.core.domain.team

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.user.User
import jakarta.persistence.*
import java.time.Duration
import java.time.LocalDateTime

/**
 * 팀 블랙리스트 엔티티
 *
 * 팀별 블랙리스트를 관리하여 재가입을 방지합니다.
 */
@Entity
@Table(
    name = "team_blacklists",
    indexes = [
        Index(name = "idx_tbl_team_id", columnList = "team_id"),
        Index(name = "idx_tbl_user_id", columnList = "user_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_tbl_team_user", columnNames = ["team_id", "user_id"]),
    ],
)
class TeamBlacklist private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Column(nullable = false, length = 1000)
    var reason: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by", nullable = false)
    val registeredBy: User,
    @Column(name = "registered_at", nullable = false)
    val registeredAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 영구 블랙리스트인지 확인합니다.
     */
    val isPermanent: Boolean
        get() = expiresAt == null

    /**
     * 만료되었는지 확인합니다.
     */
    val isExpired: Boolean
        get() = expiresAt?.isBefore(LocalDateTime.now()) == true

    /**
     * 활성 상태인지 확인합니다 (만료되지 않음).
     */
    val isActive: Boolean
        get() = !isExpired

    companion object {
        /**
         * 영구 블랙리스트를 생성합니다.
         *
         * @param team 팀
         * @param user 사용자
         * @param player 선수
         * @param reason 블랙리스트 사유
         * @param registeredBy 등록자
         * @return 생성된 TeamBlacklist
         */
        fun createPermanent(
            team: Team,
            user: User,
            player: Player,
            reason: String,
            registeredBy: User,
        ): TeamBlacklist {
            require(reason.isNotBlank()) {
                "블랙리스트 사유는 필수입니다."
            }

            return TeamBlacklist(
                team = team,
                user = user,
                player = player,
                reason = reason,
                registeredBy = registeredBy,
                registeredAt = LocalDateTime.now(),
                expiresAt = null,
            )
        }

        /**
         * 기한부 블랙리스트를 생성합니다.
         *
         * @param team 팀
         * @param user 사용자
         * @param player 선수
         * @param reason 블랙리스트 사유
         * @param registeredBy 등록자
         * @param duration 블랙리스트 기간
         * @return 생성된 TeamBlacklist
         */
        fun createTemporary(
            team: Team,
            user: User,
            player: Player,
            reason: String,
            registeredBy: User,
            duration: Duration,
        ): TeamBlacklist {
            require(reason.isNotBlank()) {
                "블랙리스트 사유는 필수입니다."
            }
            require(!duration.isNegative) {
                "블랙리스트 기간은 양수여야 합니다."
            }

            return TeamBlacklist(
                team = team,
                user = user,
                player = player,
                reason = reason,
                registeredBy = registeredBy,
                registeredAt = LocalDateTime.now(),
                expiresAt = LocalDateTime.now().plus(duration),
            )
        }
    }
}
