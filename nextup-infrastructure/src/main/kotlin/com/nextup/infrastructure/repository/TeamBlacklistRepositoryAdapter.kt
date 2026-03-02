package com.nextup.infrastructure.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.team.TeamBlacklist
import com.nextup.core.port.repository.TeamBlacklistRepositoryPort
import com.nextup.infrastructure.common.toPageResult
import com.nextup.infrastructure.common.toPageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * TeamBlacklist Repository Adapter
 * Hexagonal Architecture Outbound Adapter - TeamBlacklistRepositoryPort 구현
 */
@Repository
class TeamBlacklistRepositoryAdapter(
    private val jpaRepository: TeamBlacklistRepository,
) : TeamBlacklistRepositoryPort {
    override fun save(teamBlacklist: TeamBlacklist): TeamBlacklist = jpaRepository.save(teamBlacklist)

    override fun findByIdOrNull(id: Long): TeamBlacklist? = jpaRepository.findByIdOrNull(id)

    override fun findByTeamId(
        teamId: Long,
        pageCommand: PageCommand,
    ): PageResult<TeamBlacklist> = jpaRepository.findByTeamId(teamId, pageCommand.toPageable()).toPageResult()

    override fun findByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamBlacklist? = jpaRepository.findByTeamIdAndUserId(teamId, userId)

    override fun existsActiveByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean = jpaRepository.existsActiveByTeamIdAndUserIdWithTime(teamId, userId, LocalDateTime.now())

    override fun delete(teamBlacklist: TeamBlacklist) {
        jpaRepository.delete(teamBlacklist)
    }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}
