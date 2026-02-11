package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.attendance.ActivityScore
import org.springframework.data.jpa.repository.JpaRepository
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
}
