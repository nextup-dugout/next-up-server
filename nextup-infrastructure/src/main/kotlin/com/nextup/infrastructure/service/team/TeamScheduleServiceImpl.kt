package com.nextup.infrastructure.service.team

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.common.exception.TeamScheduleNotFoundException
import com.nextup.core.domain.team.TeamSchedule
import com.nextup.core.domain.team.TeamScheduleType
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.TeamScheduleRepositoryPort
import com.nextup.core.service.team.TeamScheduleService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 팀 일정 서비스 구현체
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class TeamScheduleServiceImpl(
    private val teamScheduleRepository: TeamScheduleRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
) : TeamScheduleService {
    @Transactional
    override fun create(
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

    override fun getByTeamId(teamId: Long): List<TeamSchedule> {
        teamRepository.findByIdOrNull(teamId)
            ?: throw TeamNotFoundException(teamId)

        return teamScheduleRepository.findByTeamId(teamId)
    }

    override fun getByTeamIdAndDateRange(
        teamId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<TeamSchedule> {
        teamRepository.findByIdOrNull(teamId)
            ?: throw TeamNotFoundException(teamId)

        return teamScheduleRepository.findByTeamIdAndDateRange(teamId, from, to)
    }

    override fun getById(id: Long): TeamSchedule =
        teamScheduleRepository.findByIdOrNull(id)
            ?: throw TeamScheduleNotFoundException(id)

    @Transactional
    override fun update(
        id: Long,
        title: String?,
        description: String?,
        scheduleType: TeamScheduleType?,
        startAt: LocalDateTime?,
        endAt: LocalDateTime?,
        location: String?,
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

    @Transactional
    override fun delete(id: Long) {
        val schedule = getById(id)
        teamScheduleRepository.deleteById(schedule.id)
    }
}
