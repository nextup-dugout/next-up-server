package com.nextup.core.domain.stadium

import com.nextup.common.exception.SlotNotAvailableException
import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * 구장 슬롯 엔티티
 *
 * 구장의 특정 날짜/시간대 예약 가능 슬롯을 관리합니다.
 */
@Entity
@Table(
    name = "stadium_slots",
    indexes = [
        Index(name = "idx_stadium_slots_stadium_date", columnList = "stadium_id,date"),
        Index(name = "idx_stadium_slots_status", columnList = "status"),
    ],
)
class StadiumSlot private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    val stadium: Stadium,
    @Column(nullable = false)
    val date: LocalDate,
    @Column(nullable = false, name = "start_time")
    val startTime: LocalTime,
    @Column(nullable = false, name = "end_time")
    val endTime: LocalTime,
    @Column(precision = 10, scale = 2)
    var price: BigDecimal? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SlotStatus = SlotStatus.AVAILABLE,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        fun create(
            stadium: Stadium,
            date: LocalDate,
            startTime: LocalTime,
            endTime: LocalTime,
            price: BigDecimal? = null,
        ): StadiumSlot {
            require(startTime.isBefore(endTime)) { "Start time must be before end time" }
            price?.let { require(it > BigDecimal.ZERO) { "Price must be positive" } }

            return StadiumSlot(
                stadium = stadium,
                date = date,
                startTime = startTime,
                endTime = endTime,
                price = price,
            )
        }
    }

    /**
     * 슬롯을 예약 상태로 변경합니다.
     */
    fun book() {
        if (status != SlotStatus.AVAILABLE) {
            throw SlotNotAvailableException(
                "SLOT_NOT_AVAILABLE",
                "Slot is not available for booking. Current status: $status",
            )
        }
        this.status = SlotStatus.BOOKED
    }

    /**
     * 예약을 취소하고 슬롯을 다시 사용 가능 상태로 만듭니다.
     */
    fun cancel() {
        if (status != SlotStatus.BOOKED) {
            throw SlotNotAvailableException(
                "SLOT_NOT_BOOKED",
                "Slot is not booked. Current status: $status",
            )
        }
        this.status = SlotStatus.AVAILABLE
    }

    /**
     * 슬롯을 유지보수 상태로 변경합니다.
     */
    fun maintain() {
        this.status = SlotStatus.MAINTENANCE
    }

    /**
     * 유지보수 상태를 해제하고 사용 가능 상태로 변경합니다.
     */
    fun endMaintenance() {
        if (status != SlotStatus.MAINTENANCE) {
            throw SlotNotAvailableException(
                "SLOT_NOT_IN_MAINTENANCE",
                "Slot is not in maintenance. Current status: $status",
            )
        }
        this.status = SlotStatus.AVAILABLE
    }
}
