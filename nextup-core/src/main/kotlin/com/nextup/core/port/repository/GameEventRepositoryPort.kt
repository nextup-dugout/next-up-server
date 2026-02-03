package com.nextup.core.port.repository

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType

/**
 * GameEvent Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface GameEventRepositoryPort {

    fun save(gameEvent: GameEvent): GameEvent

    fun saveAll(gameEvents: List<GameEvent>): List<GameEvent>

    fun findByIdOrNull(id: Long): GameEvent?

    fun findAllByGameId(gameId: Long): List<GameEvent>

    fun findAllByGameIdOrderByEventOrder(gameId: Long): List<GameEvent>

    fun findAllByGameIdAndInning(gameId: Long, inning: Int): List<GameEvent>

    fun findAllByGameIdAndInningAndIsTopInning(
        gameId: Long,
        inning: Int,
        isTopInning: Boolean
    ): List<GameEvent>

    fun findAllByGameIdAndEventType(gameId: Long, eventType: GameEventType): List<GameEvent>

    fun findLastEventByGameId(gameId: Long): GameEvent?

    fun countByGameId(gameId: Long): Long

    fun deleteById(id: Long)

    fun deleteAllByGameId(gameId: Long)
}
