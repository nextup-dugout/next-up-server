package com.nextup.core.port.repository

import com.nextup.core.domain.stadium.SlotStatus
import com.nextup.core.domain.stadium.StadiumSlot
import java.time.LocalDate

/**
 * 구장 슬롯 리포지토리 포트 인터페이스
 */
interface StadiumSlotRepositoryPort {
    fun save(slot: StadiumSlot): StadiumSlot

    fun findByIdOrNull(id: Long): StadiumSlot?

    fun findByStadiumIdAndDate(
        stadiumId: Long,
        date: LocalDate,
    ): List<StadiumSlot>

    fun findByStadiumIdAndDateBetween(
        stadiumId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<StadiumSlot>

    fun findByStadiumIdAndStatus(
        stadiumId: Long,
        status: SlotStatus,
    ): List<StadiumSlot>
}
