package com.nextup.core.service.team

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.common.exception.TeamScheduleNotFoundException
import com.nextup.core.domain.team.TeamSchedule
import com.nextup.core.domain.team.TeamScheduleType
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.TeamScheduleRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 팀 일정 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class TeamScheduleService(
    private val teamScheduleRepository: TeamScheduleRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
) {
    /**
     * 팀 일정을 생성합니다.
     */
    @Transactional
    fun create(
        teamId: Long,
        title: String,
        description: String?,
        scheduleType: TeamScheduleType,
        startAt: LocalDateTime,
        endAt: LocalDateTime?,
        location: String?,
    ): TeamSchedule {
        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)

        val schedule =
            TeamSchedule.create(
                team = team,
                title = title,
                description = description,
                scheduleType = scheduleType,
                startAt = startAt,
                endAt = endAt,
                location = location,
            )

        return teamScheduleRepository.save(schedule)
    }

    /**
     * 팀 일정 목록을 조회합니다.
     */
    fun getByTeamId(teamId: Long): List<TeamSchedule> {
        teamRepository.findByIdOrNull(teamId)
            ?: throw TeamNotFoundException(teamId)

        return teamScheduleRepository.findByTeamId(teamId)
    }

    /**
     * 팀 일정 목록을 기간으로 필터링하여 조회합니다.
     */
    fun getByTeamIdAndDateRange(
        teamId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<TeamSchedule> {
        teamRepository.findByIdOrNull(teamId)
            ?: throw TeamNotFoundException(teamId)

        return teamScheduleRepository.findByTeamIdAndDateRange(teamId, from, to)
    }

    /**
     * ID로 팀 일정을 조회합니다.
     */
    fun getById(id: Long): TeamSchedule =
        teamScheduleRepository.findByIdOrNull(id)
            ?: throw TeamScheduleNotFoundException(id)

    /**
     * 팀 일정을 수정합니다.
     */
    @Transactional
    fun update(
        id: Long,
        title: String? = null,
        description: String? = null,
        scheduleType: TeamScheduleType? = null,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
        location: String? = null,
    ): TeamSchedule {
        val schedule = getById(id)
        schedule.update(
            title = title,
            description = description,
            scheduleType = scheduleType,
            startAt = startAt,
            endAt = endAt,
            location = location,
        )
        return schedule
    }

    /**
     * 팀 일정을 삭제합니다.
     */
    @Transactional
    fun delete(id: Long) {
        val schedule = getById(id)
        teamScheduleRepository.deleteById(schedule.id)
    }
}
