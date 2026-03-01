package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * TeamMember Repository Adapter
 * Hexagonal Architecture Outbound Adapter - TeamMemberRepositoryPort 구현
 */
@Repository
class TeamMemberRepositoryAdapter(
    private val jpaRepository: TeamMemberRepository,
) : TeamMemberRepositoryPort {
    override fun save(teamMember: TeamMember): TeamMember = jpaRepository.save(teamMember)

    override fun findByIdOrNull(id: Long): TeamMember? = jpaRepository.findByIdOrNull(id)

    override fun findByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamMember? = jpaRepository.findByTeamIdAndUserId(teamId, userId)

    override fun findByTeamId(teamId: Long): List<TeamMember> = jpaRepository.findByTeamId(teamId)

    override fun findByTeamIdAndStatus(
        teamId: Long,
        status: TeamMemberStatus,
    ): List<TeamMember> = jpaRepository.findByTeamIdAndStatus(teamId, status)

    override fun findByTeamIdAndStatusIn(
        teamId: Long,
        statuses: Set<TeamMemberStatus>,
    ): List<TeamMember> = jpaRepository.findByTeamIdAndStatusIn(teamId, statuses)

    override fun findByUserId(userId: Long): List<TeamMember> = jpaRepository.findByUserId(userId)

    override fun findActiveByUserId(userId: Long): TeamMember? = jpaRepository.findActiveByUserId(userId)

    override fun findByTeamIdWithUserAndPlayer(
        teamId: Long,
        pageable: Pageable,
    ): Page<TeamMember> = jpaRepository.findByTeamIdWithUserAndPlayer(teamId, pageable)

    override fun existsByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean = jpaRepository.existsByTeamIdAndUserId(teamId, userId)

    override fun existsByTeamIdAndUniformNumberAndStatus(
        teamId: Long,
        uniformNumber: Int,
        status: TeamMemberStatus,
    ): Boolean = jpaRepository.existsByTeamIdAndUniformNumberAndStatus(teamId, uniformNumber, status)

    override fun countOwnersByTeamId(teamId: Long): Long = jpaRepository.countOwnersByTeamId(teamId)

    override fun delete(teamMember: TeamMember) {
        jpaRepository.delete(teamMember)
    }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun findByPlayerIdActive(playerId: Long): List<TeamMember> = jpaRepository.findByPlayerIdActive(playerId)

    override fun countByTeamIdAndStatus(
        teamId: Long,
        status: TeamMemberStatus,
    ): Long = jpaRepository.countByTeamIdAndStatus(teamId, status)

    override fun countByTeamIdsAndStatus(
        teamIds: List<Long>,
        status: TeamMemberStatus,
    ): Map<Long, Long> {
        if (teamIds.isEmpty()) return emptyMap()
        return jpaRepository.countByTeamIdsAndStatus(teamIds, status)
            .associate { it.getTeamId() to it.getMemberCount() }
    }
}
