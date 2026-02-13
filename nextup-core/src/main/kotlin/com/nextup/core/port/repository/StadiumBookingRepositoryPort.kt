package com.nextup.core.port.repository

import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.StadiumBooking

/**
 * 구장 예약 리포지토리 포트 인터페이스
 */
interface StadiumBookingRepositoryPort {
    fun save(booking: StadiumBooking): StadiumBooking

    fun findByIdOrNull(id: Long): StadiumBooking?

    fun findBySlotId(slotId: Long): List<StadiumBooking>

    fun findByTeamId(teamId: Long): List<StadiumBooking>

    fun findByTeamIdAndStatus(
        teamId: Long,
        status: BookingStatus,
    ): List<StadiumBooking>

    fun existsBySlotIdAndStatus(
        slotId: Long,
        status: BookingStatus,
    ): Boolean
}
