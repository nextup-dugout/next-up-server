package com.nextup.infrastructure.persistence.competition

import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import org.springframework.stereotype.Repository

@Repository
class CompetitionPlayerRepositoryAdapter(
    private val jpaRepository: CompetitionPlayerJpaRepository,
) : CompetitionPlayerRepositoryPort {
    override fun save(competitionPlayer: CompetitionPlayer): CompetitionPlayer = jpaRepository.save(competitionPlayer)

    override fun saveAll(players: List<CompetitionPlayer>): List<CompetitionPlayer> = jpaRepository.saveAll(players)

    override fun findByIdOrNull(id: Long): CompetitionPlayer? = jpaRepository.findById(id).orElse(null)

    override fun findByCompetitionId(competitionId: Long): List<CompetitionPlayer> =
        jpaRepository.findByCompetitionId(competitionId)

    override fun findByCompetitionIdAndTeamId(
        competitionId: Long,
        teamId: Long,
    ): List<CompetitionPlayer> = jpaRepository.findByCompetitionIdAndTeamId(competitionId, teamId)

    override fun findByCompetitionIdAndStatus(
        competitionId: Long,
        status: CompetitionPlayerStatus,
    ): List<CompetitionPlayer> = jpaRepository.findByCompetitionIdAndStatus(competitionId, status)

    override fun findPlayerIdsByCompetitionId(competitionId: Long): Set<Long> =
        jpaRepository.findPlayerIdsByCompetitionId(competitionId)

    override fun findEligiblePlayerIdsByCompetitionId(competitionId: Long): Set<Long> =
        jpaRepository.findEligiblePlayerIdsByCompetitionId(competitionId)

    override fun findByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): CompetitionPlayer? = jpaRepository.findByCompetitionIdAndPlayerId(competitionId, playerId)

    override fun existsByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): Boolean = jpaRepository.existsByCompetitionIdAndPlayerId(competitionId, playerId)

    override fun deleteById(id: Long) = jpaRepository.deleteById(id)

    override fun findByTeamIdAndStatus(
        teamId: Long,
        status: CompetitionPlayerStatus,
    ): List<CompetitionPlayer> = jpaRepository.findByTeamIdAndStatus(teamId, status)

    override fun findActiveCompetitionIdsByTeamId(teamId: Long): Set<Long> =
        jpaRepository.findActiveCompetitionIdsByTeamId(teamId)

    override fun findByPlayerIdAndStatusIn(
        playerId: Long,
        statuses: List<CompetitionPlayerStatus>,
    ): List<CompetitionPlayer> = jpaRepository.findByPlayerIdAndStatusIn(playerId, statuses)

    override fun findActiveByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): CompetitionPlayer? = jpaRepository.findActiveByCompetitionIdAndPlayerId(competitionId, playerId)
}
