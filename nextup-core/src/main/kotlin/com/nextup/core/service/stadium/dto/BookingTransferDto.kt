package com.nextup.core.service.stadium.dto

import java.math.BigDecimal
import java.time.Instant

/**
 * 예약 양도 등록 요청 DTO (Core 내부용)
 */
data class CreateBookingTransferRequest(
    val bookingId: Long,
    val sellerTeamId: Long,
    val transferPrice: BigDecimal?,
    val message: String?,
    val expiresAt: Instant,
)

/**
 * 예약 양도 수락 요청 DTO (Core 내부용)
 */
data class AcceptBookingTransferRequest(
    val transferId: Long,
    val buyerTeamId: Long,
)
