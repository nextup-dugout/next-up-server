package com.nextup.core.port.repository

import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.TransferStatus

/**
 * 구장 예약 양도 리포지토리 포트 인터페이스
 */
interface BookingTransferRepositoryPort {
    fun save(transfer: BookingTransfer): BookingTransfer

    fun findByIdOrNull(id: Long): BookingTransfer?

    fun findByStatus(status: TransferStatus): List<BookingTransfer>

    fun findByBookingId(bookingId: Long): List<BookingTransfer>

    fun findBySellerTeamId(sellerTeamId: Long): List<BookingTransfer>

    fun existsOpenTransferForBooking(bookingId: Long): Boolean
}
