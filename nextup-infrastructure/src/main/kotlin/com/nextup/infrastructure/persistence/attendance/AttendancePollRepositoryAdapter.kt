package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.team.Team
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AttendancePollRepositoryAdapter(
    private val jpaRepository: AttendancePollJpaRepository,
) : AttendancePollRepositoryPort {
    override fun save(attendancePoll: AttendancePoll): AttendancePoll = jpaRepository.save(attendancePoll)

    override fun findById(id: Long): AttendancePoll? = jpaRepository.findByIdOrNull(id)

    override fun findByTeam(
        team: Team,
        status: PollStatus?,
    ): List<AttendancePoll> =
        if (status == null) {
            jpaRepository.findByTeamId(team.id)
        } else {
            jpaRepository.findByTeamIdAndStatus(team.id, status)
        }

    override fun findByTeamId(
        teamId: Long,
        status: PollStatus?,
    ): List<AttendancePoll> =
        if (status == null) {
            jpaRepository.findByTeamId(teamId)
        } else {
            jpaRepository.findByTeamIdAndStatus(teamId, status)
        }

    override fun findByGameIdAndTeamId(
        gameId: Long,
        teamId: Long,
    ): AttendancePoll? = jpaRepository.findByGameIdAndTeamId(gameId, teamId)

    override fun findOpenPollsWithDeadlineBefore(deadline: LocalDateTime): List<AttendancePoll> =
        jpaRepository.findOpenPollsWithDeadlineBefore(deadline)

    override fun delete(attendancePoll: AttendancePoll) {
        jpaRepository.delete(attendancePoll)
    }

    override fun existsById(id: Long): Boolean = jpaRepository.existsById(id)

    override fun existsByGameIdAndTeamId(
        gameId: Long,
        teamId: Long,
    ): Boolean = jpaRepository.existsByGameIdAndTeamId(gameId, teamId)
}
