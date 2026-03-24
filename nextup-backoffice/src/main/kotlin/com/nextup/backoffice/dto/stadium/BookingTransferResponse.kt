package com.nextup.backoffice.dto.stadium

import com.nextup.core.domain.stadium.TransferStatus
import java.math.BigDecimal
import java.time.Instant

/**
 * 예약 양도 응답 DTO
 *
 * 변환 로직은 StadiumExtensions.kt의 Extension Function을 사용합니다.
 */
data class BookingTransferResponse(
    val id: Long,
    val bookingId: Long,
    val sellerTeamId: Long,
    val transferPrice: BigDecimal?,
    val message: String?,
    val status: TransferStatus,
    val buyerTeamId: Long?,
    val expiresAt: Instant,
    val acceptedAt: Instant?,
    val createdAt: Instant,
)
