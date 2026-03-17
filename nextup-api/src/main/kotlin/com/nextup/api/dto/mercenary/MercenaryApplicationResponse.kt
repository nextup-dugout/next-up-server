package com.nextup.api.dto.mercenary

import com.nextup.core.domain.mercenary.MercenaryApplication
import com.nextup.core.domain.mercenary.MercenaryApplicationStatus
import com.nextup.core.domain.player.Position
import java.time.Instant

data class MercenaryApplicationResponse(
    val id: Long,
    val requestId: Long,
    val playerId: Long,
    val preferredPositions: Set<Position>,
    val status: MercenaryApplicationStatus,
    val message: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(application: MercenaryApplication): MercenaryApplicationResponse =
            MercenaryApplicationResponse(
                id = application.id,
                requestId = application.requestId,
                playerId = application.playerId,
                preferredPositions = application.preferredPositions.toSet(),
                status = application.status,
                message = application.message,
                createdAt = application.createdAt,
            )
    }
}
