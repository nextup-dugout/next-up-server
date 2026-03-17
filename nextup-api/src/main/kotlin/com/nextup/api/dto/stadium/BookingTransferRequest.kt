package com.nextup.api.dto.stadium

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.Instant

/**
 * 예약 양도 등록 요청 DTO (사용자용)
 */
data class CreateBookingTransferApiRequest(
    @field:NotNull
    @field:Positive
    val bookingId: Long,
    @field:NotNull
    @field:Positive
    val sellerTeamId: Long,
    val transferPrice: BigDecimal? = null,
    val message: String? = null,
    val expiresAt: Instant? = null,
)

/**
 * 예약 양도 수락 요청 DTO (사용자용)
 */
data class AcceptBookingTransferApiRequest(
    @field:NotNull
    @field:Positive
    val buyerTeamId: Long,
)
