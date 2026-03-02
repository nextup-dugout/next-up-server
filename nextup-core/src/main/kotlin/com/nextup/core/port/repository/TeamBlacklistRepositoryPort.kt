package com.nextup.core.port.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.team.TeamBlacklist

/**
 * TeamBlacklist Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface TeamBlacklistRepositoryPort {
    fun save(teamBlacklist: TeamBlacklist): TeamBlacklist

    fun findByIdOrNull(id: Long): TeamBlacklist?

    fun findByTeamId(
        teamId: Long,
        pageCommand: PageCommand,
    ): PageResult<TeamBlacklist>

    fun findByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamBlacklist?

    fun existsActiveByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean

    fun delete(teamBlacklist: TeamBlacklist)

    fun deleteById(id: Long)
}
