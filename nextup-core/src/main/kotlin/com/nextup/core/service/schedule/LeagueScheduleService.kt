package com.nextup.core.service.schedule

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidScheduleStateException
import com.nextup.common.exception.ScheduleNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.schedule.ScheduleConflict
import com.nextup.core.domain.schedule.ScheduleConflictDetector
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.LeagueScheduleRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

/**
 * 대진표 서비스
 *
 * 대회 내 대진표 CRUD 및 상태 관리를 수행합니다.
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class LeagueScheduleService(
    private val scheduleRepository: LeagueScheduleRepositoryPort,
    private val competitionRepository: CompetitionRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
) {
    private val conflictDetector = ScheduleConflictDetector()

    /**
     * 대진표를 생성합니다.
     */
    @Transactional
    fun createSchedule(
        competitionId: Long,
        round: Int,
        matchNumber: Int,
        homeTeamId: Long,
        awayTeamId: Long,
        scheduledDate: LocalDate,
        scheduledTime: LocalTime? = null,
        venue: String? = null,
    ): LeagueSchedule {
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        val homeTeam =
            teamRepository.findByIdOrNull(homeTeamId)
                ?: throw TeamNotFoundException(homeTeamId)

        val awayTeam =
            teamRepository.findByIdOrNull(awayTeamId)
                ?: throw TeamNotFoundException(awayTeamId)

        val isDuplicate =
            scheduleRepository.existsByCompetitionIdAndRoundAndMatchNumber(competitionId, round, matchNumber)
        if (isDuplicate) {
            throw InvalidScheduleStateException(
                "이미 존재하는 대진표입니다. (대회: $competitionId, 라운드: $round, 경기번호: $matchNumber)",
            )
        }

        val schedule =
            LeagueSchedule.create(
                competition = competition,
                round = round,
                matchNumber = matchNumber,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = venue,
            )

        // 충돌 감지
        val existingSchedules =
            scheduleRepository.findByCompetitionIdAndScheduledDate(competitionId, scheduledDate)
        val conflicts = conflictDetector.detectAllConflicts(schedule, existingSchedules)

        if (conflicts.isNotEmpty()) {
            val conflictMessages =
                conflicts.joinToString("\n") { "- ${it.description}" }
            throw InvalidScheduleStateException(
                "대진표 생성 시 충돌이 감지되었습니다:\n$conflictMessages",
            )
        }

        return scheduleRepository.save(schedule)
    }

    /**
     * 대진표를 수정합니다.
     */
    @Transactional
    fun updateSchedule(
        scheduleId: Long,
        scheduledDate: LocalDate? = null,
        scheduledTime: LocalTime? = null,
        venue: String? = null,
    ): LeagueSchedule {
        val schedule = getById(scheduleId)

        if (scheduledDate != null) {
            try {
                schedule.reschedule(
                    scheduledDate,
                    scheduledTime ?: schedule.scheduledTime,
                    venue ?: schedule.venue,
                )
            } catch (e: IllegalArgumentException) {
                throw InvalidScheduleStateException(e.message ?: "일정을 변경할 수 없습니다.")
            }
        } else {
            if (venue != null) {
                schedule.venue = venue
            }
            if (scheduledTime != null) {
                schedule.scheduledTime = scheduledTime
            }
        }

        return schedule
    }

    /**
     * 대진표를 삭제합니다.
     */
    @Transactional
    fun deleteSchedule(scheduleId: Long) {
        val schedule = getById(scheduleId)
        scheduleRepository.delete(schedule)
    }

    /**
     * ID로 대진표를 조회합니다.
     */
    fun getById(scheduleId: Long): LeagueSchedule =
        scheduleRepository.findByIdOrNull(scheduleId)
            ?: throw ScheduleNotFoundException(scheduleId)

    /**
     * 대회별 전체 대진표를 조회합니다.
     */
    fun getSchedulesByCompetition(competitionId: Long): List<LeagueSchedule> =
        scheduleRepository.findByCompetitionId(competitionId)

    /**
     * 대회 + 라운드별 대진표를 조회합니다.
     */
    fun getSchedulesByRound(
        competitionId: Long,
        round: Int,
    ): List<LeagueSchedule> = scheduleRepository.findByCompetitionIdAndRound(competitionId, round)

    /**
     * 대진표를 검증합니다. (Dry-run)
     *
     * 실제로 저장하지 않고 충돌만 확인합니다.
     *
     * @return 충돌 목록 (충돌 없으면 빈 리스트)
     */
    fun validateSchedule(
        competitionId: Long,
        round: Int,
        matchNumber: Int,
        homeTeamId: Long,
        awayTeamId: Long,
        scheduledDate: LocalDate,
        scheduledTime: LocalTime? = null,
        venue: String? = null,
    ): List<ScheduleConflict> {
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        val homeTeam =
            teamRepository.findByIdOrNull(homeTeamId)
                ?: throw TeamNotFoundException(homeTeamId)

        val awayTeam =
            teamRepository.findByIdOrNull(awayTeamId)
                ?: throw TeamNotFoundException(awayTeamId)

        // 임시 스케줄 생성 (저장하지 않음)
        val schedule =
            LeagueSchedule.create(
                competition = competition,
                round = round,
                matchNumber = matchNumber,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = venue,
            )

        // 충돌 감지
        val existingSchedules =
            scheduleRepository.findByCompetitionIdAndScheduledDate(competitionId, scheduledDate)
        return conflictDetector.detectAllConflicts(schedule, existingSchedules)
    }
}
