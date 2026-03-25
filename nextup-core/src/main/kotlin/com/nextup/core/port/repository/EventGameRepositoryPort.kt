package com.nextup.core.port.repository

import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameStatus

/**
 * 이벤트 게임 리포지토리 포트
 */
interface EventGameRepositoryPort {
    fun save(eventGame: EventGame): EventGame

    fun findByIdOrNull(id: Long): EventGame?

    fun findByStatus(status: EventGameStatus): List<EventGame>

    fun findByOrganizerId(organizerId: Long): List<EventGame>
}
