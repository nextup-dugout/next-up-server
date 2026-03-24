package com.nextup.core.service.stadium

import com.nextup.common.exception.BookingNotFoundException
import com.nextup.common.exception.BookingTransferForbiddenException
import com.nextup.common.exception.BookingTransferInvalidStateException
import com.nextup.common.exception.BookingTransferNotFoundException
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.port.repository.BookingTransferRepositoryPort
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 구장 예약 양도 서비스
 *
 * 단순 양도 요청, 수락, 거절, 조회 기능을 제공합니다.
 * 가격 없이 양도 요청 -> 수락/거절만 존재합니다.
 */
@Service
@Transactional(readOnly = true)
class BookingTransferService(
    private val bookingTransferRepository: BookingTransferRepositoryPort,
    private val bookingRepository: StadiumBookingRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    /**
     * 예약 양도를 요청합니다.
     *
     * 예약 소유팀만 양도 요청이 가능하며, 예약 상태가 CONFIRMED여야 합니다.
     * 동일 예약에 대해 이미 PENDING 상태의 양도가 존재하면 안 됩니다.
     */
    @Transactional
    fun requestTransfer(
        bookingId: Long,
        fromTeamId: Long,
        toTeamId: Long,
        message: String?,
        userId: Long,
    ): BookingTransfer {
        verifyTeamMembership(fromTeamId, userId)

        val booking =
            bookingRepository.findByIdOrNull(bookingId)
                ?: throw BookingNotFoundException(bookingId)

        if (booking.teamId != fromTeamId) {
            throw BookingTransferForbiddenException(
                "Only the booking owner team can request a transfer. bookingId=$bookingId",
            )
        }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw BookingTransferInvalidStateException(
                "Booking must be CONFIRMED to request a transfer. Current status: ${booking.status}",
            )
        }

        if (bookingTransferRepository.existsPendingTransferForBooking(bookingId)) {
            throw BookingTransferInvalidStateException(
                "A pending transfer already exists for bookingId=$bookingId",
            )
        }

        val transfer =
            BookingTransfer.create(
                bookingId = bookingId,
                fromTeamId = fromTeamId,
                toTeamId = toTeamId,
                message = message,
            )

        return bookingTransferRepository.save(transfer)
    }

    /**
     * 예약 양도를 수락합니다.
     *
     * 양도 대상팀만 수락 가능합니다.
     * 수락 시 예약 소유권이 대상팀으로 이전됩니다.
     */
    @Transactional
    fun acceptTransfer(
        transferId: Long,
        userId: Long,
    ): BookingTransfer {
        val transfer =
            bookingTransferRepository.findByIdOrNull(transferId)
                ?: throw BookingTransferNotFoundException(transferId)

        verifyTeamMembership(transfer.toTeamId, userId)

        val booking =
            bookingRepository.findByIdOrNull(transfer.bookingId)
                ?: throw BookingNotFoundException(transfer.bookingId)

        transfer.accept()
        booking.transferTo(transfer.toTeamId)

        bookingTransferRepository.save(transfer)
        bookingRepository.save(booking)

        return transfer
    }

    /**
     * 예약 양도를 거절합니다.
     *
     * 양도 대상팀만 거절 가능합니다.
     */
    @Transactional
    fun rejectTransfer(
        transferId: Long,
        userId: Long,
    ): BookingTransfer {
        val transfer =
            bookingTransferRepository.findByIdOrNull(transferId)
                ?: throw BookingTransferNotFoundException(transferId)

        verifyTeamMembership(transfer.toTeamId, userId)

        transfer.reject()
        return bookingTransferRepository.save(transfer)
    }

    /**
     * 특정 팀이 보낸 양도 요청 목록을 조회합니다.
     */
    fun getSentTransfers(teamId: Long): List<BookingTransfer> = bookingTransferRepository.findByFromTeamId(teamId)

    /**
     * 특정 팀이 받은 양도 요청 목록을 조회합니다.
     */
    fun getReceivedTransfers(teamId: Long): List<BookingTransfer> = bookingTransferRepository.findByToTeamId(teamId)

    private fun verifyTeamMembership(
        teamId: Long,
        userId: Long,
    ) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            ?: throw BookingTransferForbiddenException(
                "User $userId is not a member of team $teamId",
            )
    }
}
