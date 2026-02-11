package com.nextup.core.service.schedule

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidScheduleStateException
import com.nextup.common.exception.ScheduleNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.schedule.RoundRobinScheduleGenerator
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
    private val scheduleGenerator = RoundRobinScheduleGenerator()

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

    /**
     * 라운드 로빈 방식으로 대진표를 자동 생성합니다.
     *
     * @param competitionId 대회 ID
     * @param teamIds 참가 팀 ID 목록
     * @param doubleRoundRobin true면 홈/원정 교대로 2회전 (기본 false)
     * @return 생성된 대진표 목록
     */
    @Transactional
    fun generateRoundRobinSchedule(
        competitionId: Long,
        teamIds: List<Long>,
        doubleRoundRobin: Boolean = false,
    ): List<LeagueSchedule> {
        // 대회 존재 확인
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        // 팀 존재 확인
        val teams =
            teamIds.map { teamId ->
                teamRepository.findByIdOrNull(teamId)
                    ?: throw TeamNotFoundException(teamId)
            }

        // 라운드 로빈 매치 생성
        val matchPairs = scheduleGenerator.generate(teamIds, doubleRoundRobin)

        // 라운드별 매치 카운터
        val matchNumbersByRound = mutableMapOf<Int, Int>()

        // LeagueSchedule 엔티티 생성 (날짜는 임시로 현재 날짜 + 라운드 offset)
        val schedules =
            matchPairs.map { match ->
                val homeTeam = teams.first { it.id == match.homeTeamId }
                val awayTeam = teams.first { it.id == match.awayTeamId }

                // 라운드별 매치 번호 증가
                val matchNumber = matchNumbersByRound.getOrDefault(match.round, 0) + 1
                matchNumbersByRound[match.round] = matchNumber

                // 임시 날짜: 현재 날짜 + (라운드 - 1) * 7일 (1주일 간격)
                val scheduledDate = LocalDate.now().plusWeeks((match.round - 1).toLong())

                LeagueSchedule.create(
                    competition = competition,
                    round = match.round,
                    matchNumber = matchNumber,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledDate = scheduledDate,
                )
            }

        // 배치 저장
        return schedules.map { scheduleRepository.save(it) }
    }

    /**
     * 특정 날짜의 모든 경기를 일괄 연기합니다. (우천 순연 등)
     */
    @Transactional
    fun postponeGamesBulk(
        competitionId: Long,
        date: LocalDate,
        reason: String,
    ): List<LeagueSchedule> {
        val schedules =
            scheduleRepository.findByCompetitionIdAndScheduledDate(competitionId, date)

        if (schedules.isEmpty()) {
            throw InvalidScheduleStateException(
                "해당 날짜에 연기할 경기가 없습니다. (대회: $competitionId, 날짜: $date)",
            )
        }

        schedules.forEach { schedule ->
            try {
                schedule.postpone(reason)
            } catch (e: IllegalArgumentException) {
                throw InvalidScheduleStateException(
                    "대진표를 연기할 수 없습니다. (ID: ${schedule.id}, 사유: ${e.message})",
                )
            }
        }

        return schedules
    }

    /**
     * 연기된 경기의 일정을 재조정합니다.
     */
    @Transactional
    fun rescheduleGame(
        scheduleId: Long,
        newDate: LocalDate,
        newVenue: String? = null,
    ): LeagueSchedule {
        val schedule = getById(scheduleId)

        try {
            schedule.reschedule(
                newDate = newDate,
                newTime = schedule.scheduledTime,
                newVenue = newVenue ?: schedule.venue,
            )
        } catch (e: IllegalArgumentException) {
            throw InvalidScheduleStateException(
                e.message ?: "일정을 재조정할 수 없습니다.",
            )
        }

        val existingSchedules =
            scheduleRepository
                .findByCompetitionIdAndScheduledDate(schedule.competition.id, newDate)
                .filter { it.id != scheduleId }

        val conflicts = conflictDetector.detectAllConflicts(schedule, existingSchedules)

        if (conflicts.isNotEmpty()) {
            val conflictMessages =
                conflicts.joinToString("\n") { "- ${it.description}" }
            throw InvalidScheduleStateException(
                "일정 재조정 시 충돌이 감지되었습니다:\n$conflictMessages",
            )
        }

        return schedule
    }
}
