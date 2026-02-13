package com.nextup.api.dto.stadium

import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.StadiumBooking
import java.time.Instant

/**
 * 구장 예약 응답 DTO
 */
data class BookingResponse(
    val id: Long,
    val slotId: Long,
    val teamId: Long,
    val bookedBy: Long,
    val status: BookingStatus,
    val bookedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(booking: StadiumBooking): BookingResponse =
            BookingResponse(
                id = booking.id,
                slotId = booking.slot.id,
                teamId = booking.teamId,
                bookedBy = booking.bookedBy,
                status = booking.status,
                bookedAt = booking.bookedAt,
                createdAt = booking.createdAt,
                updatedAt = booking.updatedAt,
            )
    }
}
