package com.nextup.scorer.dto.game

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 몰수 처리 요청 DTO
 */
data class ForfeitRequestDto(
    @field:NotNull(message = "승리팀 ID는 필수입니다")
    val winnerTeamId: Long?,
    @field:NotBlank(message = "몰수 사유는 필수입니다")
    val reason: String?,
)
