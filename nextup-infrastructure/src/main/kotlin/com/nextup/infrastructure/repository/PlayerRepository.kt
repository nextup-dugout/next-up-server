package com.nextup.infrastructure.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.infrastructure.common.toPageResult
import com.nextup.infrastructure.common.toPageable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    @Query(
        """
        SELECT DISTINCT p FROM Player p
        LEFT JOIN PlayerTeamHistory h ON h.player.id = p.id AND h.endDate IS NULL
        WHERE (:name IS NULL OR p.name LIKE %:name%)
        AND (:teamId IS NULL OR h.team.id = :teamId)
        AND (:position IS NULL OR p.primaryPosition = :position)
    """,
    )
    fun searchByPageable(
        @Param("name") name: String?,
        @Param("teamId") teamId: Long?,
        @Param("position") position: Position?,
        pageable: Pageable,
    ): Page<Player>

    override fun search(
        name: String?,
        teamId: Long?,
        position: Position?,
        pageCommand: PageCommand,
    ): PageResult<Player> = searchByPageable(name, teamId, position, pageCommand.toPageable()).toPageResult()
}
