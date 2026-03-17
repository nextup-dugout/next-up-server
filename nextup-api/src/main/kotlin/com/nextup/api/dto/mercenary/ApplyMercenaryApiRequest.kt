package com.nextup.api.dto.mercenary

import com.nextup.core.domain.player.Position
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class ApplyMercenaryApiRequest(
    @field:NotNull(message = "선수 ID는 필수입니다")
    val playerId: Long,
    @field:NotEmpty(message = "선호 포지션을 최소 1개 이상 지정해야 합니다")
    val preferredPositions: Set<Position>,
    val message: String? = null,
)
