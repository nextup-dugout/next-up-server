package com.nextup.core.port.repository

import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.schedule.ScheduleStatus

/**
 * LeagueSchedule Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface LeagueScheduleRepositoryPort {
    fun save(schedule: LeagueSchedule): LeagueSchedule

    fun findByIdOrNull(id: Long): LeagueSchedule?

    fun delete(schedule: LeagueSchedule)

    fun deleteById(id: Long)

    /**
     * 대회별 대진표 전체 조회
     */
    fun findByCompetitionId(competitionId: Long): List<LeagueSchedule>

    /**
     * 대회별 + 라운드별 대진표 조회
     */
    fun findByCompetitionIdAndRound(
        competitionId: Long,
        round: Int
    ): List<LeagueSchedule>

    /**
     * 대회별 + 상태별 대진표 조회
     */
    fun findByCompetitionIdAndStatus(
        competitionId: Long,
        status: ScheduleStatus
    ): List<LeagueSchedule>

    /**
     * 날짜 범위로 대진표 조회
     */
    fun findByScheduledDateBetween(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ): List<LeagueSchedule>

    /**
     * 중복 대진표 확인
     */
    fun existsByCompetitionIdAndRoundAndMatchNumber(
        competitionId: Long,
        round: Int,
        matchNumber: Int,
    ): Boolean

    /**
     * 대회별 + 날짜별 대진표 조회 (충돌 감지용)
     */
    fun findByCompetitionIdAndScheduledDate(
        competitionId: Long,
        scheduledDate: java.time.LocalDate,
    ): List<LeagueSchedule>
}
