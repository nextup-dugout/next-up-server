package com.nextup.backoffice.dto.stadium

import com.nextup.core.domain.stadium.TransferStatus
import java.time.Instant

/**
 * 예약 양도 응답 DTO (관리자용)
 *
 * 변환 로직은 StadiumExtensions.kt의 Extension Function을 사용합니다.
 */
data class BookingTransferResponse(
    val id: Long,
    val bookingId: Long,
    val fromTeamId: Long,
    val toTeamId: Long,
    val message: String?,
    val status: TransferStatus,
    val createdAt: Instant,
)
