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
}
