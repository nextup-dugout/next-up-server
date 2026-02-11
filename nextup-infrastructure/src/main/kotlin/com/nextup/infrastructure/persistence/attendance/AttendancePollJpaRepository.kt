package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.PollStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AttendancePollJpaRepository : JpaRepository<AttendancePoll, Long> {
    fun findByTeamId(teamId: Long): List<AttendancePoll>

    fun findByTeamIdAndStatus(
        teamId: Long,
        status: PollStatus,
    ): List<AttendancePoll>
}
