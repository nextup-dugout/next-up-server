package com.nextup.api.dto.stadium

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * 예약 양도 요청 DTO (사용자용)
 */
data class CreateBookingTransferApiRequest(
    @field:NotNull
    @field:Positive
    val bookingId: Long,
    @field:NotNull
    @field:Positive
    val fromTeamId: Long,
    @field:NotNull
    @field:Positive
    val toTeamId: Long,
    @field:Size(max = 500)
    val message: String? = null,
)
