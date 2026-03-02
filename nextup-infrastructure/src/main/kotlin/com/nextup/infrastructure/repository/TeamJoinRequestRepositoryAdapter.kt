package com.nextup.infrastructure.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.domain.team.TeamJoinRequest
import com.nextup.core.port.repository.TeamJoinRequestRepositoryPort
import com.nextup.infrastructure.common.toPageResult
import com.nextup.infrastructure.common.toPageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * TeamJoinRequest Repository Adapter
 * Hexagonal Architecture Outbound Adapter - TeamJoinRequestRepositoryPort 구현
 */
@Repository
class TeamJoinRequestRepositoryAdapter(
    private val jpaRepository: TeamJoinRequestRepository,
) : TeamJoinRequestRepositoryPort {
    override fun save(teamJoinRequest: TeamJoinRequest): TeamJoinRequest = jpaRepository.save(teamJoinRequest)

    override fun findByIdOrNull(id: Long): TeamJoinRequest? = jpaRepository.findByIdOrNull(id)

    override fun findByTeamId(teamId: Long): List<TeamJoinRequest> = jpaRepository.findByTeamId(teamId)

    override fun findByTeamIdAndStatus(
        teamId: Long,
        status: JoinRequestStatus,
        pageCommand: PageCommand,
    ): PageResult<TeamJoinRequest> =
        jpaRepository.findByTeamIdAndStatus(teamId, status, pageCommand.toPageable()).toPageResult()

    override fun findByUserId(userId: Long): List<TeamJoinRequest> = jpaRepository.findByUserId(userId)

    override fun findPendingByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamJoinRequest? = jpaRepository.findPendingByTeamIdAndUserId(teamId, userId)

    override fun existsPendingByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean = jpaRepository.existsPendingByTeamIdAndUserId(teamId, userId)

    override fun delete(teamJoinRequest: TeamJoinRequest) {
        jpaRepository.delete(teamJoinRequest)
    }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}
