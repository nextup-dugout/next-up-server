package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.TeamBlacklist
import com.nextup.core.port.repository.TeamBlacklistRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

/**
 * TeamBlacklist Repository
 * JpaRepository 상속 + Port 인터페이스 구현
 */
interface TeamBlacklistRepository :
    JpaRepository<TeamBlacklist, Long>,
    TeamBlacklistRepositoryPort {
    override fun findByIdOrNull(id: Long): TeamBlacklist? = findById(id).orElse(null)

    @Query("SELECT tbl FROM TeamBlacklist tbl WHERE tbl.team.id = :teamId")
    override fun findByTeamId(
        teamId: Long,
        pageable: Pageable,
    ): Page<TeamBlacklist>

    @Query("SELECT tbl FROM TeamBlacklist tbl WHERE tbl.team.id = :teamId AND tbl.user.id = :userId")
    override fun findByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamBlacklist?

    @Query(
        """
        SELECT CASE WHEN COUNT(tbl) > 0 THEN true ELSE false END
        FROM TeamBlacklist tbl
        WHERE tbl.team.id = :teamId
        AND tbl.user.id = :userId
        AND (tbl.expiresAt IS NULL OR tbl.expiresAt > :now)
        """,
    )
    override fun existsActiveByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean {
        return existsActiveByTeamIdAndUserIdWithTime(teamId, userId, LocalDateTime.now())
    }

    @Query(
        """
        SELECT CASE WHEN COUNT(tbl) > 0 THEN true ELSE false END
        FROM TeamBlacklist tbl
        WHERE tbl.team.id = :teamId
        AND tbl.user.id = :userId
        AND (tbl.expiresAt IS NULL OR tbl.expiresAt > :now)
        """,
    )
    fun existsActiveByTeamIdAndUserIdWithTime(
        teamId: Long,
        userId: Long,
        now: LocalDateTime,
    ): Boolean
}
