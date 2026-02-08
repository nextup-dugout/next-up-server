package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * TeamMember Repository
 * JpaRepository 상속 + Port 인터페이스 구현
 */
interface TeamMemberRepository :
    JpaRepository<TeamMember, Long>,
    TeamMemberRepositoryPort {
    override fun findByIdOrNull(id: Long): TeamMember? = findById(id).orElse(null)

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.user.id = :userId")
    override fun findByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamMember?

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId")
    override fun findByTeamId(teamId: Long): List<TeamMember>

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.status = :status")
    override fun findByTeamIdAndStatus(
        teamId: Long,
        status: TeamMemberStatus,
    ): List<TeamMember>

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.status IN :statuses")
    override fun findByTeamIdAndStatusIn(
        teamId: Long,
        statuses: Set<TeamMemberStatus>,
    ): List<TeamMember>

    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId")
    override fun findByUserId(userId: Long): List<TeamMember>

    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.status = 'ACTIVE'")
    override fun findActiveByUserId(userId: Long): TeamMember?

    @Query(
        """
        SELECT tm FROM TeamMember tm
        JOIN FETCH tm.user
        JOIN FETCH tm.player
        WHERE tm.team.id = :teamId
        """,
    )
    override fun findByTeamIdWithUserAndPlayer(
        teamId: Long,
        pageable: Pageable,
    ): Page<TeamMember>

    @Query(
        "SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.user.id = :userId"
    )
    override fun existsByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.uniformNumber = :uniformNumber AND tm.status = :status"
    )
    override fun existsByTeamIdAndUniformNumberAndStatus(
        teamId: Long,
        uniformNumber: Int,
        status: TeamMemberStatus,
    ): Boolean

    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role = 'OWNER'")
    override fun countOwnersByTeamId(teamId: Long): Long
}
