package com.nextup.core.domain.discipline

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * 선수 제재(BAN) 엔티티
 *
 * 기존 Discipline 엔티티에서 WARNING/SUSPENSION을 삭제하고,
 * BAN만 남긴 최소 엔티티입니다.
 *
 * 5필드: playerId, competitionId, reason, issuedBy, issuedAt
 */
@Entity
@Table(
    name = "player_bans",
    indexes = [
        Index(name = "idx_player_bans_player_id", columnList = "player_id"),
        Index(name = "idx_player_bans_competition_id", columnList = "competition_id"),
    ],
)
class PlayerBan private constructor(
    @Column(name = "player_id", nullable = false)
    val playerId: Long,
    @Column(name = "competition_id", nullable = false)
    val competitionId: Long,
    @Column(nullable = false, columnDefinition = "TEXT")
    val reason: String,
    @Column(nullable = false, name = "issued_by", length = 255)
    val issuedBy: String,
    @Column(nullable = false, name = "issued_at")
    val issuedAt: Instant = Instant.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        /**
         * 영구 제재를 생성합니다.
         */
        fun create(
            playerId: Long,
            competitionId: Long,
            reason: String,
            issuedBy: String,
        ): PlayerBan {
            require(reason.isNotBlank()) { "제재 사유는 필수입니다." }
            require(issuedBy.isNotBlank()) { "제재 발급자는 필수입니다." }

            return PlayerBan(
                playerId = playerId,
                competitionId = competitionId,
                reason = reason,
                issuedBy = issuedBy,
            )
        }
    }
}
