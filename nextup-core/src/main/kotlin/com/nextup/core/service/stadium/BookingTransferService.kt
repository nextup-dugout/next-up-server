package com.nextup.core.service.stadium

import com.nextup.common.exception.BookingNotFoundException
import com.nextup.common.exception.BookingTransferForbiddenException
import com.nextup.common.exception.BookingTransferInvalidStateException
import com.nextup.common.exception.BookingTransferNotFoundException
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.TransferStatus
import com.nextup.core.port.repository.BookingTransferRepositoryPort
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * 구장 예약 양도 서비스
 *
 * 긴급 양도 등록, 양수 수락, 양도 취소, 양도 가능 목록 조회 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class BookingTransferService(
    private val bookingTransferRepository: BookingTransferRepositoryPort,
    private val bookingRepository: StadiumBookingRepositoryPort,
) {
    /**
     * 예약 양도를 등록합니다.
     *
     * 예약 소유팀만 양도 등록이 가능하며, 예약 상태가 CONFIRMED여야 합니다.
     * 동일 예약에 대해 이미 OPEN 상태의 양도가 존재하면 안 됩니다.
     */
    @Transactional
    fun createTransfer(
        bookingId: Long,
        teamId: Long,
        price: BigDecimal?,
        message: String?,
        expiresAt: Instant = Instant.now().plusSeconds(DEFAULT_EXPIRY_SECONDS),
    ): BookingTransfer {
        val booking =
            bookingRepository.findByIdOrNull(bookingId)
                ?: throw BookingNotFoundException(bookingId)

        if (booking.teamId != teamId) {
            throw BookingTransferForbiddenException(
                "Only the booking owner team can register a transfer. bookingId=$bookingId",
            )
        }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw BookingTransferInvalidStateException(
                "Booking must be CONFIRMED to register a transfer. Current status: ${booking.status}",
            )
        }

        if (bookingTransferRepository.existsOpenTransferForBooking(bookingId)) {
            throw BookingTransferInvalidStateException(
                "An open transfer already exists for bookingId=$bookingId",
            )
        }

        val transfer =
            BookingTransfer.create(
                bookingId = bookingId,
                sellerTeamId = teamId,
                transferPrice = price,
                message = message,
                expiresAt = expiresAt,
            )

        return bookingTransferRepository.save(transfer)
    }

    /**
     * 예약 양도를 수락합니다.
     *
     * 양도를 수락하면 예약 소유권이 구매팀으로 이전됩니다.
     */
    @Transactional
    fun acceptTransfer(
        transferId: Long,
        buyerTeamId: Long,
    ): BookingTransfer {
        val transfer =
            bookingTransferRepository.findByIdOrNull(transferId)
                ?: throw BookingTransferNotFoundException(transferId)

        val booking =
            bookingRepository.findByIdOrNull(transfer.bookingId)
                ?: throw BookingNotFoundException(transfer.bookingId)

        transfer.accept(buyerTeamId)
        booking.transferTo(buyerTeamId)

        bookingTransferRepository.save(transfer)
        bookingRepository.save(booking)

        return transfer
    }

    /**
     * 예약 양도를 취소합니다.
     *
     * 양도 등록 팀만 취소 가능합니다.
     */
    @Transactional
    fun cancelTransfer(
        transferId: Long,
        teamId: Long,
    ): BookingTransfer {
        val transfer =
            bookingTransferRepository.findByIdOrNull(transferId)
                ?: throw BookingTransferNotFoundException(transferId)

        if (transfer.sellerTeamId != teamId) {
            throw BookingTransferForbiddenException(
                "Only the seller team can cancel the transfer. transferId=$transferId",
            )
        }

        transfer.cancel()
        return bookingTransferRepository.save(transfer)
    }

    /**
     * 양도 가능한(OPEN 상태) 예약 양도 목록을 조회합니다.
     */
    fun getAvailableTransfers(): List<BookingTransfer> =
        bookingTransferRepository.findByStatus(TransferStatus.OPEN)
            .filter { !it.isExpired() }

    companion object {
        const val DEFAULT_EXPIRY_SECONDS = 24L * 60 * 60 // 24 hours
    }
}
