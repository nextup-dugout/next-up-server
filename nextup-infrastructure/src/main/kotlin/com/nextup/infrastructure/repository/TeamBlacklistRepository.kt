package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.TeamBlacklist
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

/**
 * TeamBlacklist JPA Repository
 * JPA 전용 쿼리 메서드 정의
 */
interface TeamBlacklistRepository : JpaRepository<TeamBlacklist, Long> {
    @Query("SELECT tbl FROM TeamBlacklist tbl WHERE tbl.team.id = :teamId")
    fun findByTeamId(
        teamId: Long,
        pageable: Pageable,
    ): Page<TeamBlacklist>

    @Query("SELECT tbl FROM TeamBlacklist tbl WHERE tbl.team.id = :teamId AND tbl.user.id = :userId")
    fun findByTeamIdAndUserId(
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
    fun existsActiveByTeamIdAndUserIdWithTime(
        teamId: Long,
        userId: Long,
        now: LocalDateTime,
    ): Boolean
}
