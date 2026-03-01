package com.nextup.core.port.repository

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * TeamMember Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface TeamMemberRepositoryPort {
    fun save(teamMember: TeamMember): TeamMember

    fun findByIdOrNull(id: Long): TeamMember?

    fun findByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamMember?

    fun findByTeamId(teamId: Long): List<TeamMember>

    fun findByTeamIdAndStatus(
        teamId: Long,
        status: TeamMemberStatus,
    ): List<TeamMember>

    fun findByTeamIdAndStatusIn(
        teamId: Long,
        statuses: Set<TeamMemberStatus>,
    ): List<TeamMember>

    fun findByUserId(userId: Long): List<TeamMember>

    fun findActiveByUserId(userId: Long): TeamMember?

    fun findByTeamIdWithUserAndPlayer(
        teamId: Long,
        pageable: Pageable,
    ): Page<TeamMember>

    fun existsByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean

    fun existsByTeamIdAndUniformNumberAndStatus(
        teamId: Long,
        uniformNumber: Int,
        status: TeamMemberStatus,
    ): Boolean

    fun countOwnersByTeamId(teamId: Long): Long

    fun delete(teamMember: TeamMember)

    fun deleteById(id: Long)

    fun findByPlayerIdActive(playerId: Long): List<TeamMember>

    fun findByPlayerIdsActive(playerIds: List<Long>): List<TeamMember>

    fun countByTeamIdAndStatus(
        teamId: Long,
        status: TeamMemberStatus,
    ): Long
}
