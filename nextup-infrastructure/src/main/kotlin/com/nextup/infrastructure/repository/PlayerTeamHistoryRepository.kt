package com.nextup.infrastructure.repository

import com.nextup.core.domain.player.PlayerTeamHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface PlayerTeamHistoryRepository : JpaRepository<PlayerTeamHistory, Long> {

    fun findByPlayerId(playerId: Long): List<PlayerTeamHistory>

    fun findByTeamId(teamId: Long): List<PlayerTeamHistory>

    @Query("SELECT h FROM PlayerTeamHistory h WHERE h.player.id = :playerId AND h.endDate IS NULL")
    fun findCurrentByPlayerId(playerId: Long): PlayerTeamHistory?

    @Query("""
        SELECT h FROM PlayerTeamHistory h
        WHERE h.team.id = :teamId AND h.endDate IS NULL
    """)
    fun findCurrentByTeamId(teamId: Long): List<PlayerTeamHistory>

    @Query("""
        SELECT h FROM PlayerTeamHistory h
        JOIN FETCH h.player
        JOIN FETCH h.team
        WHERE h.player.id = :playerId
        ORDER BY h.startDate DESC
    """)
    fun findByPlayerIdWithDetails(playerId: Long): List<PlayerTeamHistory>

    @Query("""
        SELECT h FROM PlayerTeamHistory h
        WHERE h.team.id = :teamId
        AND h.startDate <= :date
        AND (h.endDate IS NULL OR h.endDate >= :date)
    """)
    fun findByTeamIdAtDate(teamId: Long, date: LocalDate): List<PlayerTeamHistory>
}
