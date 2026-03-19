package com.nextup.core.port.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberStatus

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

    fun findByTeamIdInAndStatus(
        teamIds: List<Long>,
        status: TeamMemberStatus,
    ): List<TeamMember>

    fun findByTeamIdAndStatusIn(
        teamId: Long,
        statuses: Set<TeamMemberStatus>,
    ): List<TeamMember>

    fun findByUserId(userId: Long): List<TeamMember>

    fun findActiveByUserId(userId: Long): TeamMember?

    fun findAllActiveByUserId(userId: Long): List<TeamMember>

    fun findByTeamIdWithUserAndPlayer(
        teamId: Long,
        pageCommand: PageCommand,
    ): PageResult<TeamMember>

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

    fun countByTeamIdsAndStatus(
        teamIds: List<Long>,
        status: TeamMemberStatus,
    ): Map<Long, Long>
}
