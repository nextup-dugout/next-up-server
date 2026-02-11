package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.StadiumBooking
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface StadiumBookingJpaRepository : JpaRepository<StadiumBooking, Long> {
    @Query("SELECT b FROM StadiumBooking b WHERE b.slot.id = :slotId")
    fun findBySlotId(slotId: Long): List<StadiumBooking>

    @Query("SELECT b FROM StadiumBooking b WHERE b.teamId = :teamId")
    fun findByTeamId(teamId: Long): List<StadiumBooking>

    @Query("SELECT b FROM StadiumBooking b WHERE b.teamId = :teamId AND b.status = :status")
    fun findByTeamIdAndStatus(
        teamId: Long,
        status: BookingStatus,
    ): List<StadiumBooking>

    @Query(
        "SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM StadiumBooking b WHERE b.slot.id = :slotId AND b.status = :status"
    )
    fun existsBySlotIdAndStatus(
        slotId: Long,
        status: BookingStatus,
    ): Boolean
}
