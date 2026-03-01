package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * TeamMember JPA Repository
 * JPA 전용 쿼리 메서드 정의
 */
interface TeamMemberRepository : JpaRepository<TeamMember, Long> {
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.user.id = :userId")
    fun findByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamMember?

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId")
    fun findByTeamId(teamId: Long): List<TeamMember>

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.status = :status")
    fun findByTeamIdAndStatus(
        teamId: Long,
        status: TeamMemberStatus,
    ): List<TeamMember>

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.status IN :statuses")
    fun findByTeamIdAndStatusIn(
        teamId: Long,
        statuses: Set<TeamMemberStatus>,
    ): List<TeamMember>

    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId")
    fun findByUserId(userId: Long): List<TeamMember>

    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.status = 'ACTIVE'")
    fun findActiveByUserId(userId: Long): TeamMember?

    @Query(
        """
        SELECT tm FROM TeamMember tm
        JOIN FETCH tm.user
        JOIN FETCH tm.player
        WHERE tm.team.id = :teamId
        """,
    )
    fun findByTeamIdWithUserAndPlayer(
        teamId: Long,
        pageable: Pageable,
    ): Page<TeamMember>

    @Query(
        "SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.user.id = :userId"
    )
    fun existsByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.uniformNumber = :uniformNumber AND tm.status = :status"
    )
    fun existsByTeamIdAndUniformNumberAndStatus(
        teamId: Long,
        uniformNumber: Int,
        status: TeamMemberStatus,
    ): Boolean

    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role = 'OWNER'")
    fun countOwnersByTeamId(teamId: Long): Long

    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.team WHERE tm.player.id = :playerId AND tm.status = 'ACTIVE'")
    fun findByPlayerIdActive(playerId: Long): List<TeamMember>

    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.status = :status")
    fun countByTeamIdAndStatus(
        teamId: Long,
        status: TeamMemberStatus,
    ): Long

    @Query(
        """
        SELECT tm.team.id AS teamId, COUNT(tm) AS memberCount
        FROM TeamMember tm
        WHERE tm.team.id IN :teamIds AND tm.status = :status
        GROUP BY tm.team.id
        """,
    )
    fun countByTeamIdsAndStatus(
        teamIds: List<Long>,
        status: TeamMemberStatus,
    ): List<TeamMemberCountProjection>
}
