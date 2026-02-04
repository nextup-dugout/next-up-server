package com.nextup.core.port.repository

import com.nextup.core.domain.game.GameEvent

/**
 * GameEvent Repository Port
 */
interface GameEventRepositoryPort {
    fun save(gameEvent: GameEvent): GameEvent

    fun findByIdOrNull(id: Long): GameEvent?

    fun findAllByGameId(gameId: Long): List<GameEvent>

    fun findAllByGameIdOrderByEventTimestamp(gameId: Long): List<GameEvent>

    fun delete(gameEvent: GameEvent)
}
