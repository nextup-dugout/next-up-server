package com.nextup.api.dto.stadium

import com.nextup.core.domain.stadium.SlotStatus
import com.nextup.core.domain.stadium.StadiumSlot
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * 구장 슬롯 응답 DTO
 */
data class StadiumSlotResponse(
    val id: Long,
    val stadiumId: Long,
    val stadiumName: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val price: BigDecimal?,
    val status: SlotStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(slot: StadiumSlot): StadiumSlotResponse =
            StadiumSlotResponse(
                id = slot.id,
                stadiumId = slot.stadium.id,
                stadiumName = slot.stadium.name,
                date = slot.date,
                startTime = slot.startTime,
                endTime = slot.endTime,
                price = slot.price,
                status = slot.status,
                createdAt = slot.createdAt,
                updatedAt = slot.updatedAt,
            )
    }
}
