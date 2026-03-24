package com.nextup.core.service.stadium.dto

/**
 * 예약 양도 요청 DTO (Core 내부용)
 */
data class CreateBookingTransferRequest(
    val bookingId: Long,
    val fromTeamId: Long,
    val toTeamId: Long,
    val message: String?,
)
