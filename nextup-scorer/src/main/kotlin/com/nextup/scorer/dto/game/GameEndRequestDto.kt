package com.nextup.scorer.dto.game

import com.nextup.core.service.game.dto.GameEndReason
import jakarta.validation.constraints.NotNull

/**
 * 경기 종료 요청 DTO
 */
data class GameEndRequestDto(
    @field:NotNull(message = "종료 사유는 필수입니다")
    val reason: GameEndReason?
)
