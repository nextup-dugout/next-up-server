package com.nextup.infrastructure.persistence.eventgame

import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameStatus
import com.nextup.core.port.repository.EventGameRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface EventGameJpaRepository :
    JpaRepository<EventGame, Long>,
    EventGameRepositoryPort {
    override fun findByStatus(status: EventGameStatus): List<EventGame>

    override fun findByOrganizerId(organizerId: Long): List<EventGame>

    override fun findByIdOrNull(id: Long): EventGame? = findById(id).orElse(null)
}
