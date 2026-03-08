package com.nextup.api.dto.stadium

import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.StadiumBooking
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * 구장 예약 상세 조회 응답 DTO
 *
 * GET /api/v1/bookings/{id} 에서 사용됩니다.
 * 구장 정보, 슬롯 시간, 예약 상태를 포함합니다.
 */
data class BookingDetailResponse(
    val id: Long,
    val status: BookingStatus,
    val teamId: Long,
    val bookedBy: Long,
    val bookedAt: Instant,
    val stadium: StadiumInfo,
    val slot: SlotInfo,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    data class StadiumInfo(
        val id: Long,
        val name: String,
        val address: String,
    )

    data class SlotInfo(
        val id: Long,
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val price: BigDecimal?,
    )

    companion object {
        fun from(booking: StadiumBooking): BookingDetailResponse =
            BookingDetailResponse(
                id = booking.id,
                status = booking.status,
                teamId = booking.teamId,
                bookedBy = booking.bookedBy,
                bookedAt = booking.bookedAt,
                stadium =
                    StadiumInfo(
                        id = booking.slot.stadium.id,
                        name = booking.slot.stadium.name,
                        address = booking.slot.stadium.address,
                    ),
                slot =
                    SlotInfo(
                        id = booking.slot.id,
                        date = booking.slot.date,
                        startTime = booking.slot.startTime,
                        endTime = booking.slot.endTime,
                        price = booking.slot.price,
                    ),
                createdAt = booking.createdAt,
                updatedAt = booking.updatedAt,
            )
    }
}
