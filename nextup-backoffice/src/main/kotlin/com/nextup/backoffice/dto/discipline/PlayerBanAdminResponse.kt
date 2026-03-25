package com.nextup.backoffice.dto.discipline

import com.nextup.core.domain.discipline.PlayerBan
import java.time.Instant

/**
 * 선수 제재 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 */
data class PlayerBanAdminResponse(
    val id: Long,
    val playerId: Long,
    val competitionId: Long,
    val reason: String,
    val issuedBy: String,
    val issuedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(ban: PlayerBan): PlayerBanAdminResponse =
            PlayerBanAdminResponse(
                id = ban.id,
                playerId = ban.playerId,
                competitionId = ban.competitionId,
                reason = ban.reason,
                issuedBy = ban.issuedBy,
                issuedAt = ban.issuedAt,
                createdAt = ban.createdAt,
                updatedAt = ban.updatedAt,
            )
    }
}
