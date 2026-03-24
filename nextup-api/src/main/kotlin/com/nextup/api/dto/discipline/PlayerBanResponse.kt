package com.nextup.api.dto.discipline

import com.nextup.core.domain.discipline.PlayerBan
import java.time.Instant

/**
 * 선수 제재 응답 DTO (공개 API용)
 */
data class PlayerBanResponse(
    val id: Long,
    val playerId: Long,
    val competitionId: Long,
    val reason: String,
    val issuedBy: String,
    val issuedAt: Instant,
) {
    companion object {
        fun from(ban: PlayerBan): PlayerBanResponse =
            PlayerBanResponse(
                id = ban.id,
                playerId = ban.playerId,
                competitionId = ban.competitionId,
                reason = ban.reason,
                issuedBy = ban.issuedBy,
                issuedAt = ban.issuedAt,
            )
    }
}

/**
 * 선수 제재 이력 응답 DTO
 */
data class PlayerBanHistoryResponse(
    val playerId: Long,
    val totalBans: Int,
    val bans: List<PlayerBanResponse>,
)
