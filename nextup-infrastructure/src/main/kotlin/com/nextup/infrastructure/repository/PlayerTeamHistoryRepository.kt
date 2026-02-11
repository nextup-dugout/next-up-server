package com.nextup.infrastructure.repository

import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.domain.player.PlayerTeamStatus
import com.nextup.core.port.repository.PlayerTeamHistoryRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface PlayerTeamHistoryRepository :
    JpaRepository<PlayerTeamHistory, Long>,
    PlayerTeamHistoryRepositoryPort {
    override fun findByPlayerId(playerId: Long): List<PlayerTeamHistory>

    override fun findByTeamId(teamId: Long): List<PlayerTeamHistory>

    @Query("SELECT h FROM PlayerTeamHistory h WHERE h.player.id = :playerId AND h.endDate IS NULL")
    override fun findCurrentByPlayerId(playerId: Long): PlayerTeamHistory?

    @Query(
        """
        SELECT h FROM PlayerTeamHistory h
        WHERE h.team.id = :teamId AND h.endDate IS NULL
    """,
    )
    override fun findCurrentByTeamId(teamId: Long): List<PlayerTeamHistory>

    @Query(
        """
        SELECT h FROM PlayerTeamHistory h
        JOIN FETCH h.player
        JOIN FETCH h.team
        WHERE h.player.id = :playerId
        ORDER BY h.startDate DESC
    """,
    )
    override fun findByPlayerIdWithDetails(playerId: Long): List<PlayerTeamHistory>

    @Query(
        """
        SELECT h FROM PlayerTeamHistory h
        WHERE h.team.id = :teamId
        AND h.startDate <= :date
        AND (h.endDate IS NULL OR h.endDate >= :date)
    """,
    )
    override fun findByTeamIdAtDate(
        teamId: Long,
        date: LocalDate,
    ): List<PlayerTeamHistory>

    // ===== 신규 메서드 (Issue #37) =====

    @Query(
        """
        SELECT h FROM PlayerTeamHistory h
        WHERE h.player.id = :playerId
        AND h.status = 'ACTIVE'
    """,
    )
    override fun findActiveByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<PlayerTeamHistory>

    @Query(
        """
        SELECT h FROM PlayerTeamHistory h
        JOIN h.team t
        WHERE h.player.id = :playerId
        AND t.league.id = :leagueId
        AND h.status = 'ACTIVE'
    """,
    )
    override fun findActiveByPlayerIdAndLeagueId(
        @Param("playerId") playerId: Long,
        @Param("leagueId") leagueId: Long,
    ): PlayerTeamHistory?

    @Query(
        """
        SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
        FROM PlayerTeamHistory h
        JOIN h.team t
        WHERE h.player.id = :playerId
        AND t.league.id = :leagueId
        AND h.status = 'ACTIVE'
    """,
    )
    override fun existsActiveByPlayerIdAndLeagueId(
        @Param("playerId") playerId: Long,
        @Param("leagueId") leagueId: Long,
    ): Boolean

    @Query(
        """
        SELECT h FROM PlayerTeamHistory h
        WHERE h.player.id = :playerId
        AND h.status = :status
    """,
    )
    override fun findByPlayerIdAndStatus(
        @Param("playerId") playerId: Long,
        @Param("status") status: PlayerTeamStatus,
    ): List<PlayerTeamHistory>

    @Query(
        """
        SELECT h FROM PlayerTeamHistory h
        WHERE h.team.id = :teamId
        AND h.status = 'ACTIVE'
    """,
    )
    override fun findActiveByTeamId(
        @Param("teamId") teamId: Long,
    ): List<PlayerTeamHistory>
}
