package com.nextup.core.port.repository

import com.nextup.core.domain.eventgame.EventGameParticipant

/**
 * 이벤트 게임 참가자 리포지토리 포트
 */
interface EventGameParticipantRepositoryPort {
    fun save(participant: EventGameParticipant): EventGameParticipant

    fun findByIdOrNull(id: Long): EventGameParticipant?

    fun findByEventGameId(eventGameId: Long): List<EventGameParticipant>

    fun findByPlayerId(playerId: Long): List<EventGameParticipant>
}
