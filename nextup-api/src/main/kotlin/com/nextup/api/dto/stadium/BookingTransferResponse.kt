package com.nextup.api.dto.stadium

import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.TransferStatus
import java.math.BigDecimal
import java.time.Instant

/**
 * 예약 양도 응답 DTO (사용자용)
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
) {
    companion object {
        fun from(transfer: BookingTransfer): BookingTransferResponse =
            BookingTransferResponse(
                id = transfer.id,
                bookingId = transfer.bookingId,
                sellerTeamId = transfer.sellerTeamId,
                transferPrice = transfer.transferPrice,
                message = transfer.message,
                status = transfer.status,
                buyerTeamId = transfer.buyerTeamId,
                expiresAt = transfer.expiresAt,
                acceptedAt = transfer.acceptedAt,
                createdAt = transfer.createdAt,
            )
    }
}
