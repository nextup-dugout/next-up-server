package com.nextup.api.dto.mercenary

import com.nextup.core.domain.mercenary.MercenaryParticipation
import java.time.Instant

data class MercenaryParticipationResponse(
    val id: Long,
    val gameId: Long,
    val playerId: Long,
    val teamId: Long,
    val createdAt: Instant,
) {
    companion object {
        fun from(participation: MercenaryParticipation): MercenaryParticipationResponse =
            MercenaryParticipationResponse(
                id = participation.id,
                gameId = participation.gameId,
                playerId = participation.playerId,
                teamId = participation.teamId,
                createdAt = participation.createdAt,
            )
    }
}
