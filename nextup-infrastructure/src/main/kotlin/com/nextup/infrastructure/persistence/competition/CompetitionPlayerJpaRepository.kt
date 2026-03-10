package com.nextup.infrastructure.persistence.competition

import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CompetitionPlayerJpaRepository : JpaRepository<CompetitionPlayer, Long> {
    fun findByCompetitionId(competitionId: Long): List<CompetitionPlayer>

    fun findByCompetitionIdAndTeamId(
        competitionId: Long,
        teamId: Long,
    ): List<CompetitionPlayer>

    fun findByCompetitionIdAndStatus(
        competitionId: Long,
        status: CompetitionPlayerStatus,
    ): List<CompetitionPlayer>

    @Query("SELECT cp.player.id FROM CompetitionPlayer cp WHERE cp.competition.id = :competitionId")
    fun findPlayerIdsByCompetitionId(competitionId: Long): Set<Long>

    @Query(
        "SELECT cp.player.id FROM CompetitionPlayer cp " +
            "WHERE cp.competition.id = :competitionId AND cp.status = 'ACTIVE'",
    )
    fun findEligiblePlayerIdsByCompetitionId(competitionId: Long): Set<Long>

    fun findByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): CompetitionPlayer?

    fun existsByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): Boolean

    fun findByTeamIdAndStatus(
        teamId: Long,
        status: CompetitionPlayerStatus,
    ): List<CompetitionPlayer>

    @Query(
        "SELECT DISTINCT cp.competition.id FROM CompetitionPlayer cp " +
            "WHERE cp.team.id = :teamId AND cp.status <> 'WITHDRAWN'",
    )
    fun findActiveCompetitionIdsByTeamId(teamId: Long): Set<Long>
}
