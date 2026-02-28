package com.nextup.backoffice.dto.stadium

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.Instant

/**
 * 예약 양도 등록 요청
 */
data class CreateBookingTransferRequest(
    @field:NotNull
    @field:Positive
    val sellerTeamId: Long,
    val transferPrice: BigDecimal? = null,
    val message: String? = null,
    val expiresAt: Instant? = null,
)

/**
 * 예약 양도 수락 요청
 */
data class AcceptBookingTransferRequest(
    @field:NotNull
    @field:Positive
    val buyerTeamId: Long,
)

/**
 * 예약 양도 취소 요청
 */
data class CancelBookingTransferRequest(
    @field:NotNull
    @field:Positive
    val teamId: Long,
)
