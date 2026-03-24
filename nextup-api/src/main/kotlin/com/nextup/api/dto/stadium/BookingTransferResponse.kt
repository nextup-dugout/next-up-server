package com.nextup.api.dto.stadium

import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.TransferStatus
import java.time.Instant

/**
 * 예약 양도 응답 DTO (사용자용)
 */
data class BookingTransferResponse(
    val id: Long,
    val bookingId: Long,
    val fromTeamId: Long,
    val toTeamId: Long,
    val message: String?,
    val status: TransferStatus,
    val createdAt: Instant,
) {
    companion object {
        fun from(transfer: BookingTransfer): BookingTransferResponse =
            BookingTransferResponse(
                id = transfer.id,
                bookingId = transfer.bookingId,
                fromTeamId = transfer.fromTeamId,
                toTeamId = transfer.toTeamId,
                message = transfer.message,
                status = transfer.status,
                createdAt = transfer.createdAt,
            )
    }
}
