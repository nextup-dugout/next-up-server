package com.nextup.api.dto.mercenary

import com.nextup.core.domain.mercenary.MercenaryRequest
import com.nextup.core.domain.mercenary.MercenaryRequestStatus
import com.nextup.core.domain.player.Position
import java.time.Instant

data class MercenaryRequestResponse(
    val id: Long,
    val requestingTeamId: Long,
    val gameId: Long,
    val positions: Set<Position>,
    val maxCount: Int,
    val status: MercenaryRequestStatus,
    val deadline: Instant,
    val description: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(request: MercenaryRequest): MercenaryRequestResponse =
            MercenaryRequestResponse(
                id = request.id,
                requestingTeamId = request.requestingTeamId,
                gameId = request.gameId,
                positions = request.positions.toSet(),
                maxCount = request.maxCount,
                status = request.status,
                deadline = request.deadline,
                description = request.description,
                createdAt = request.createdAt,
            )
    }
}
