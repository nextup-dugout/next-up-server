package com.nextup.infrastructure.repository

import com.nextup.core.domain.player.Player
import com.nextup.core.port.repository.PlayerRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface PlayerRepository :
    JpaRepository<Player, Long>,
    PlayerRepositoryPort {
    override fun findByName(name: String): List<Player>

    override fun findByNameContaining(name: String): List<Player>

    @Query("SELECT p FROM Player p WHERE p.retirementYear IS NULL")
    override fun findActivePlayers(): List<Player>

    @Query(
        """
        SELECT p FROM Player p
        JOIN FETCH p._teamHistories h
        WHERE h.team.id = :teamId AND h.endDate IS NULL
    """,
    )
    override fun findCurrentPlayersByTeamId(teamId: Long): List<Player>

    @Query(
        """
        SELECT p FROM Player p
        JOIN p._teamHistories h
        WHERE h.team.id = :teamId
        AND h.startDate <= :date
        AND (h.endDate IS NULL OR h.endDate >= :date)
    """,
    )
    override fun findPlayersByTeamIdAtDate(
        teamId: Long,
        date: LocalDate,
    ): List<Player>
}
