package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.port.repository.GameEventRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GameEventRepository :
    JpaRepository<GameEvent, Long>,
    GameEventRepositoryPort {
    override fun findByIdOrNull(id: Long): GameEvent? = findById(id).orElse(null)

    @Query(
        """
        SELECT ge FROM GameEvent ge
        JOIN FETCH ge.game g
        WHERE ge.pitcher.player.id = :pitcherId
          AND ge.batter.player.id = :batterId
          AND ge.eventType = 'PLATE_APPEARANCE'
          AND ge.plateAppearanceResult IS NOT NULL
        ORDER BY ge.eventTimestamp
        """,
    )
    override fun findPlateAppearancesByPitcherAndBatter(
        pitcherId: Long,
        batterId: Long,
    ): List<GameEvent>

    @Query(
        """
        SELECT ge FROM GameEvent ge
        JOIN FETCH ge.game g
        WHERE ge.pitcher.player.id = :pitcherId
          AND ge.batter.player.id = :batterId
          AND ge.eventType = 'PLATE_APPEARANCE'
          AND ge.plateAppearanceResult IS NOT NULL
          AND YEAR(g.scheduledAt) = :year
        ORDER BY ge.eventTimestamp
        """,
    )
    override fun findPlateAppearancesByPitcherAndBatterAndYear(
        pitcherId: Long,
        batterId: Long,
        year: Int,
    ): List<GameEvent>
}
