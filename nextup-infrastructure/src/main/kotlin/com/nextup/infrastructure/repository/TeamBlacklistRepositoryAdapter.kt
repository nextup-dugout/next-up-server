package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.TeamBlacklist
import com.nextup.core.port.repository.TeamBlacklistRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
        pageable: Pageable,
    ): Page<TeamBlacklist> = jpaRepository.findByTeamId(teamId, pageable)

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
