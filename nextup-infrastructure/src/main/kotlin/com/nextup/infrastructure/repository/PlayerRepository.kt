package com.nextup.infrastructure.repository

import com.nextup.core.domain.player.Player
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface PlayerRepository : JpaRepository<Player, Long> {

    fun findByName(name: String): List<Player>

    fun findByNameContaining(name: String): List<Player>

    @Query("SELECT p FROM Player p WHERE p.retirementYear IS NULL")
    fun findActivePlayers(): List<Player>

    @Query("""
        SELECT p FROM Player p
        JOIN FETCH p._teamHistories h
        WHERE h.team.id = :teamId AND h.endDate IS NULL
    """)
    fun findCurrentPlayersByTeamId(teamId: Long): List<Player>

    @Query("""
        SELECT p FROM Player p
        JOIN p._teamHistories h
        WHERE h.team.id = :teamId
        AND h.startDate <= :date
        AND (h.endDate IS NULL OR h.endDate >= :date)
    """)
    fun findPlayersByTeamIdAtDate(teamId: Long, date: LocalDate): List<Player>
}
