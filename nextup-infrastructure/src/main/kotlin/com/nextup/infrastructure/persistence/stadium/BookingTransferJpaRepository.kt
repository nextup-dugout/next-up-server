package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.TransferStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BookingTransferJpaRepository : JpaRepository<BookingTransfer, Long> {
    @Query("SELECT t FROM BookingTransfer t WHERE t.status = :status")
    fun findByStatus(status: TransferStatus): List<BookingTransfer>

    @Query("SELECT t FROM BookingTransfer t WHERE t.bookingId = :bookingId")
    fun findByBookingId(bookingId: Long): List<BookingTransfer>

    @Query("SELECT t FROM BookingTransfer t WHERE t.fromTeamId = :fromTeamId")
    fun findByFromTeamId(fromTeamId: Long): List<BookingTransfer>

    @Query("SELECT t FROM BookingTransfer t WHERE t.toTeamId = :toTeamId")
    fun findByToTeamId(toTeamId: Long): List<BookingTransfer>

    @Query(
        "SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM BookingTransfer t WHERE t.bookingId = :bookingId AND t.status = 'PENDING'",
    )
    fun existsPendingTransferForBooking(bookingId: Long): Boolean
}
