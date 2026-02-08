package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.domain.team.TeamJoinRequest
import com.nextup.core.port.repository.TeamJoinRequestRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * TeamJoinRequest Repository
 * JpaRepository 상속 + Port 인터페이스 구현
 */
interface TeamJoinRequestRepository :
    JpaRepository<TeamJoinRequest, Long>,
    TeamJoinRequestRepositoryPort {
    override fun findByIdOrNull(id: Long): TeamJoinRequest? = findById(id).orElse(null)

    @Query("SELECT tjr FROM TeamJoinRequest tjr WHERE tjr.team.id = :teamId")
    override fun findByTeamId(teamId: Long): List<TeamJoinRequest>

    @Query("SELECT tjr FROM TeamJoinRequest tjr WHERE tjr.team.id = :teamId AND tjr.status = :status")
    override fun findByTeamIdAndStatus(
        teamId: Long,
        status: JoinRequestStatus,
        pageable: Pageable,
    ): Page<TeamJoinRequest>

    @Query("SELECT tjr FROM TeamJoinRequest tjr WHERE tjr.user.id = :userId")
    override fun findByUserId(userId: Long): List<TeamJoinRequest>

    @Query(
        "SELECT tjr FROM TeamJoinRequest tjr WHERE tjr.team.id = :teamId AND tjr.user.id = :userId AND tjr.status = 'PENDING'"
    )
    override fun findPendingByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamJoinRequest?

    @Query(
        "SELECT CASE WHEN COUNT(tjr) > 0 THEN true ELSE false END FROM TeamJoinRequest tjr WHERE tjr.team.id = :teamId AND tjr.user.id = :userId AND tjr.status = 'PENDING'"
    )
    override fun existsPendingByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean
}
