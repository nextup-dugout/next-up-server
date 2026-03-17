package com.nextup.core.service.mercenary.dto

import com.nextup.core.domain.player.Position
import java.time.Instant

data class CreateMercenaryRequestDto(
    val requestingTeamId: Long,
    val gameId: Long,
    val positions: Set<Position>,
    val maxCount: Int,
    val deadline: Instant,
    val description: String? = null,
)
