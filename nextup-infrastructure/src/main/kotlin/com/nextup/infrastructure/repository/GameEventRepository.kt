package com.nextup.infrastructure.repository

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.port.repository.GameEventRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface GameEventRepository : JpaRepository<GameEvent, Long>, GameEventRepositoryPort {

    override fun findByIdOrNull(id: Long): GameEvent? = findById(id).orElse(null)

    @Query("SELECT e FROM GameEvent e WHERE e.game.id = :gameId")
    override fun findAllByGameId(gameId: Long): List<GameEvent>

    @Query("SELECT e FROM GameEvent e WHERE e.game.id = :gameId ORDER BY e.eventOrder ASC")
    override fun findAllByGameIdOrderByEventOrder(gameId: Long): List<GameEvent>

    @Query("""
        SELECT e FROM GameEvent e
        WHERE e.game.id = :gameId AND e.inning = :inning
        ORDER BY e.eventOrder ASC
    """)
    override fun findAllByGameIdAndInning(gameId: Long, inning: Int): List<GameEvent>

    @Query("""
        SELECT e FROM GameEvent e
        WHERE e.game.id = :gameId AND e.inning = :inning AND e.isTopInning = :isTopInning
        ORDER BY e.eventOrder ASC
    """)
    override fun findAllByGameIdAndInningAndIsTopInning(
        gameId: Long,
        inning: Int,
        isTopInning: Boolean
    ): List<GameEvent>

    @Query("""
        SELECT e FROM GameEvent e
        WHERE e.game.id = :gameId AND e.eventType = :eventType
        ORDER BY e.eventOrder ASC
    """)
    override fun findAllByGameIdAndEventType(gameId: Long, eventType: GameEventType): List<GameEvent>

    @Query("""
        SELECT e FROM GameEvent e
        WHERE e.game.id = :gameId
        ORDER BY e.eventOrder DESC
        LIMIT 1
    """)
    override fun findLastEventByGameId(gameId: Long): GameEvent?

    @Query("SELECT COUNT(e) FROM GameEvent e WHERE e.game.id = :gameId")
    override fun countByGameId(gameId: Long): Long

    @Modifying
    @Query("DELETE FROM GameEvent e WHERE e.game.id = :gameId")
    override fun deleteAllByGameId(gameId: Long)
}
