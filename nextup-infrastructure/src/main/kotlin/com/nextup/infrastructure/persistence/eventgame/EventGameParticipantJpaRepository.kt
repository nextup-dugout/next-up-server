package com.nextup.infrastructure.persistence.eventgame

import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.port.repository.EventGameParticipantRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface EventGameParticipantJpaRepository :
    JpaRepository<EventGameParticipant, Long>,
    EventGameParticipantRepositoryPort {
    override fun findByEventGameId(eventGameId: Long): List<EventGameParticipant>

    override fun findByPlayerId(playerId: Long): List<EventGameParticipant>

    override fun findByIdOrNull(id: Long): EventGameParticipant? = findById(id).orElse(null)
}
