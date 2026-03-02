package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.attendance.ActivityScore
import com.nextup.core.domain.team.TeamMemberStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ActivityScoreJpaRepository : JpaRepository<ActivityScore, Long> {
    fun findByTeamId(teamId: Long): List<ActivityScore>

    fun findByTeamIdAndMemberId(
        teamId: Long,
        memberId: Long,
    ): ActivityScore?

    fun existsByTeamIdAndMemberId(
        teamId: Long,
        memberId: Long,
    ): Boolean

    /**
     * 팀의 활성(ACTIVE) 멤버 활동 점수만 조회합니다.
     * LEFT/KICKED 멤버의 점수는 이력으로 보존되지만 활성 통계에서 제외됩니다.
     */
    @Query(
        """
        SELECT a FROM ActivityScore a
        WHERE a.team.id = :teamId
        AND a.member.status = :status
        """,
    )
    fun findByTeamIdAndMemberStatus(
        @Param("teamId") teamId: Long,
        @Param("status") status: TeamMemberStatus,
    ): List<ActivityScore>
}
