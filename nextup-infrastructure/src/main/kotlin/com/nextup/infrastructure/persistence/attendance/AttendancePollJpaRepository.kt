package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.PollStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AttendancePollJpaRepository : JpaRepository<AttendancePoll, Long> {
    fun findByTeamId(teamId: Long): List<AttendancePoll>

    fun findByTeamIdAndStatus(
        teamId: Long,
        status: PollStatus,
    ): List<AttendancePoll>

    fun findByGameIdAndTeamId(
        gameId: Long,
        teamId: Long,
    ): AttendancePoll?

    fun existsByGameIdAndTeamId(
        gameId: Long,
        teamId: Long,
    ): Boolean

    @Query("SELECT p FROM AttendancePoll p WHERE p.status = 'OPEN' AND p.deadline < :deadline")
    fun findOpenPollsWithDeadlineBefore(deadline: LocalDateTime): List<AttendancePoll>
}
