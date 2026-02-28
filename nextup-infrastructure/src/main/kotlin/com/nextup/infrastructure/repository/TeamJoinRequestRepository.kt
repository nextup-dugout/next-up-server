package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.domain.team.TeamJoinRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * TeamJoinRequest JPA Repository
 * JPA 전용 쿼리 메서드 정의
 */
interface TeamJoinRequestRepository : JpaRepository<TeamJoinRequest, Long> {
    @Query("SELECT tjr FROM TeamJoinRequest tjr WHERE tjr.team.id = :teamId")
    fun findByTeamId(teamId: Long): List<TeamJoinRequest>

    @Query("SELECT tjr FROM TeamJoinRequest tjr WHERE tjr.team.id = :teamId AND tjr.status = :status")
    fun findByTeamIdAndStatus(
        teamId: Long,
        status: JoinRequestStatus,
        pageable: Pageable,
    ): Page<TeamJoinRequest>

    @Query("SELECT tjr FROM TeamJoinRequest tjr WHERE tjr.user.id = :userId")
    fun findByUserId(userId: Long): List<TeamJoinRequest>

    @Query(
        "SELECT tjr FROM TeamJoinRequest tjr WHERE tjr.team.id = :teamId AND tjr.user.id = :userId AND tjr.status = 'PENDING'"
    )
    fun findPendingByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): TeamJoinRequest?

    @Query(
        "SELECT CASE WHEN COUNT(tjr) > 0 THEN true ELSE false END FROM TeamJoinRequest tjr WHERE tjr.team.id = :teamId AND tjr.user.id = :userId AND tjr.status = 'PENDING'"
    )
    fun existsPendingByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean
}
