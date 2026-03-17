package com.nextup.core.service.mercenary.dto

import com.nextup.core.domain.player.Position

data class ApplyMercenaryDto(
    val requestId: Long,
    val playerId: Long,
    val preferredPositions: Set<Position>,
    val message: String? = null,
)
