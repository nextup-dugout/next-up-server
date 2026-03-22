package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class StadiumBookingRepositoryAdapter(
    private val jpaRepository: StadiumBookingJpaRepository,
) : StadiumBookingRepositoryPort {
    override fun save(booking: StadiumBooking): StadiumBooking = jpaRepository.save(booking)

    override fun findByIdOrNull(id: Long): StadiumBooking? = jpaRepository.findByIdOrNull(id)

    override fun findBySlotId(slotId: Long): List<StadiumBooking> = jpaRepository.findBySlotId(slotId)

    override fun findByTeamId(teamId: Long): List<StadiumBooking> = jpaRepository.findByTeamId(teamId)

    override fun findByTeamIdAndStatus(
        teamId: Long,
        status: BookingStatus,
    ): List<StadiumBooking> = jpaRepository.findByTeamIdAndStatus(teamId, status)

    override fun existsBySlotIdAndStatus(
        slotId: Long,
        status: BookingStatus,
    ): Boolean = jpaRepository.existsBySlotIdAndStatus(slotId, status)

    override fun findByStadiumIdAndStatus(
        stadiumId: Long,
        status: BookingStatus,
    ): List<StadiumBooking> = jpaRepository.findByStadiumIdAndStatus(stadiumId, status)
}
