package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.TransferStatus
import com.nextup.core.port.repository.BookingTransferRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class BookingTransferRepositoryAdapter(
    private val jpaRepository: BookingTransferJpaRepository,
) : BookingTransferRepositoryPort {
    override fun save(transfer: BookingTransfer): BookingTransfer = jpaRepository.save(transfer)

    override fun findByIdOrNull(id: Long): BookingTransfer? = jpaRepository.findByIdOrNull(id)

    override fun findByStatus(status: TransferStatus): List<BookingTransfer> = jpaRepository.findByStatus(status)

    override fun findByBookingId(bookingId: Long): List<BookingTransfer> = jpaRepository.findByBookingId(bookingId)

    override fun findByFromTeamId(fromTeamId: Long): List<BookingTransfer> = jpaRepository.findByFromTeamId(fromTeamId)

    override fun findByToTeamId(toTeamId: Long): List<BookingTransfer> = jpaRepository.findByToTeamId(toTeamId)

    override fun existsPendingTransferForBooking(bookingId: Long): Boolean =
        jpaRepository.existsPendingTransferForBooking(bookingId)
}
