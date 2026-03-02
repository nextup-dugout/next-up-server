package com.nextup.core.port.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.domain.team.TeamJoinRequest

/**
 * TeamJoinRequest Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface TeamJoinRequestRepositoryPort {
    fun save(teamJoinRequest: TeamJoinRequest): TeamJoinRequest

    fun findByIdOrNull(id: Long): TeamJoinRequest?

    fun findByTeamId(teamId: Long): List<TeamJoinRequest>

    fun findByTeamIdAndStatus(
        teamId: Long,
        status: JoinRequestStatus,
        pageCommand: PageCommand,
    ): PageResult<TeamJoinRequest>

    fun findByUserId(userId: Long): List<TeamJoinRequest>

    fun findPendingByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamJoinRequest?

    fun existsPendingByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean

    fun delete(teamJoinRequest: TeamJoinRequest)

    fun deleteById(id: Long)
}
