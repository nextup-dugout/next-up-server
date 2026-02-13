package com.nextup.core.domain.stadium

import com.nextup.common.exception.InvalidStateException
import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * 구장 예약 엔티티
 *
 * 팀의 구장 슬롯 예약 정보를 관리합니다.
 */
@Entity
@Table(
    name = "stadium_bookings",
    indexes = [
        Index(name = "idx_stadium_bookings_slot", columnList = "slot_id"),
        Index(name = "idx_stadium_bookings_team", columnList = "team_id"),
        Index(name = "idx_stadium_bookings_status", columnList = "status"),
    ],
)
class StadiumBooking private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    val slot: StadiumSlot,
    @Column(nullable = false, name = "team_id")
    val teamId: Long,
    @Column(nullable = false, name = "booked_by")
    val bookedBy: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BookingStatus = BookingStatus.CONFIRMED,
    @Column(nullable = false, name = "booked_at")
    val bookedAt: Instant = Instant.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        fun create(
            slot: StadiumSlot,
            teamId: Long,
            bookedBy: Long,
        ): StadiumBooking {
            require(teamId > 0) { "Team ID must be positive" }
            require(bookedBy > 0) { "Booked by user ID must be positive" }

            return StadiumBooking(
                slot = slot,
                teamId = teamId,
                bookedBy = bookedBy,
            )
        }
    }

    /**
     * 예약을 취소합니다.
     */
    fun cancel() {
        if (status != BookingStatus.CONFIRMED) {
            throw InvalidStateException(
                "BOOKING_CANNOT_BE_CANCELLED",
                "Booking cannot be cancelled. Current status: $status",
            )
        }
        this.status = BookingStatus.CANCELLED
    }

    /**
     * 예약을 완료 처리합니다.
     */
    fun complete() {
        if (status != BookingStatus.CONFIRMED) {
            throw InvalidStateException(
                "BOOKING_CANNOT_BE_COMPLETED",
                "Booking cannot be completed. Current status: $status",
            )
        }
        this.status = BookingStatus.COMPLETED
    }
}
