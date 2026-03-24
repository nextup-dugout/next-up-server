package com.nextup.backoffice.dto.stadium

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * 예약 양도 요청 (관리자용)
 */
data class CreateBookingTransferRequest(
    @field:NotNull
    @field:Positive
    val fromTeamId: Long,
    @field:NotNull
    @field:Positive
    val toTeamId: Long,
    @field:Size(max = 500)
    val message: String? = null,
)
