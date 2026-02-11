package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.GameTeam
import com.nextup.core.port.repository.GameTeamRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GameTeamRepository :
    JpaRepository<GameTeam, Long>,
    GameTeamRepositoryPort {
    override fun findByIdOrNull(id: Long): GameTeam? = findById(id).orElse(null)

    override fun findAllByTeamId(teamId: Long): List<GameTeam> = findByTeamId(teamId)

    override fun findAllByTeamIdAndYear(
        teamId: Long,
        year: Int,
    ): List<GameTeam> = findByTeamIdAndGameScheduledAtYear(teamId, year)

    override fun findAllByTeamIdAndCompetitionId(
        teamId: Long,
        competitionId: Long,
    ): List<GameTeam> = findByTeamIdAndGameCompetitionId(teamId, competitionId)

    override fun findAllByGameId(gameId: Long): List<GameTeam> = findByGameId(gameId)

    // Spring Data JPA 쿼리 메서드
    fun findByTeamId(teamId: Long): List<GameTeam>

    @Query(
        """
        SELECT gt FROM GameTeam gt
        WHERE gt.team.id = :teamId
        AND YEAR(gt.game.scheduledAt) = :year
        """,
    )
    fun findByTeamIdAndGameScheduledAtYear(
        teamId: Long,
        year: Int,
    ): List<GameTeam>

    @Query(
        """
        SELECT gt FROM GameTeam gt
        WHERE gt.team.id = :teamId
        AND gt.game.competition.id = :competitionId
        """,
    )
    fun findByTeamIdAndGameCompetitionId(
        teamId: Long,
        competitionId: Long,
    ): List<GameTeam>

    fun findByGameId(gameId: Long): List<GameTeam>

    override fun findAllByGameIds(gameIds: List<Long>): List<GameTeam> = findByGameIdIn(gameIds)

    fun findByGameIdIn(gameIds: List<Long>): List<GameTeam>

    @Query(
        """
        SELECT gt FROM GameTeam gt
        JOIN FETCH gt.team
        JOIN FETCH gt.game g
        WHERE g.competition.id = :competitionId
        AND gt.result != 'UNDECIDED'
        """,
    )
    override fun findAllByCompetitionIdWithDecidedResult(competitionId: Long): List<GameTeam>

    @Query(
        """
        SELECT gt FROM GameTeam gt
        JOIN FETCH gt.team
        JOIN FETCH gt.game g
        WHERE g.competition.id = :competitionId
        """,
    )
    override fun findAllByCompetitionId(competitionId: Long): List<GameTeam>

    @Query(
        """
        SELECT gt FROM GameTeam gt
        JOIN FETCH gt.game g
        JOIN FETCH gt.team t
        WHERE gt.team.id = :teamId
        AND g.status IN ('FINISHED', 'CALLED', 'FORFEITED')
        AND EXISTS (
            SELECT 1 FROM GameTeam gt2
            WHERE gt2.game.id = gt.game.id
            AND gt2.team.id = :opponentId
        )
        AND (:competitionId IS NULL OR g.competition.id = :competitionId)
        ORDER BY g.scheduledAt DESC
        """,
    )
    override fun findCompletedGamesBetweenTeams(
        teamId: Long,
        opponentId: Long,
        competitionId: Long?,
    ): List<GameTeam>
}
