package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.SlotStatus
import com.nextup.core.domain.stadium.StadiumSlot
import com.nextup.core.port.repository.StadiumSlotRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class StadiumSlotRepositoryAdapter(
    private val jpaRepository: StadiumSlotJpaRepository,
) : StadiumSlotRepositoryPort {
    override fun save(slot: StadiumSlot): StadiumSlot = jpaRepository.save(slot)

    override fun findByIdOrNull(id: Long): StadiumSlot? = jpaRepository.findByIdOrNull(id)

    override fun findByStadiumIdAndDate(
        stadiumId: Long,
        date: LocalDate,
    ): List<StadiumSlot> = jpaRepository.findByStadiumIdAndDate(stadiumId, date)

    override fun findByStadiumIdAndDateBetween(
        stadiumId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<StadiumSlot> = jpaRepository.findByStadiumIdAndDateBetween(stadiumId, startDate, endDate)

    override fun findByStadiumIdAndStatus(
        stadiumId: Long,
        status: SlotStatus,
    ): List<StadiumSlot> = jpaRepository.findByStadiumIdAndStatus(stadiumId, status)
}
