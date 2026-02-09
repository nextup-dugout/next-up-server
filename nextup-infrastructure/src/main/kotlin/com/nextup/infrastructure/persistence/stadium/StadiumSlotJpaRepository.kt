package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.SlotStatus
import com.nextup.core.domain.stadium.StadiumSlot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface StadiumSlotJpaRepository : JpaRepository<StadiumSlot, Long> {
    @Query("SELECT s FROM StadiumSlot s WHERE s.stadium.id = :stadiumId AND s.date = :date")
    fun findByStadiumIdAndDate(
        stadiumId: Long,
        date: LocalDate,
    ): List<StadiumSlot>

    @Query("SELECT s FROM StadiumSlot s WHERE s.stadium.id = :stadiumId AND s.date BETWEEN :startDate AND :endDate")
    fun findByStadiumIdAndDateBetween(
        stadiumId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<StadiumSlot>

    @Query("SELECT s FROM StadiumSlot s WHERE s.stadium.id = :stadiumId AND s.status = :status")
    fun findByStadiumIdAndStatus(
        stadiumId: Long,
        status: SlotStatus,
    ): List<StadiumSlot>
}
