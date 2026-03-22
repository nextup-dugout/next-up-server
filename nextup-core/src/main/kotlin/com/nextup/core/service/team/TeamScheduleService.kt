package com.nextup.core.service.team

import com.nextup.core.domain.team.TeamSchedule
import com.nextup.core.domain.team.TeamScheduleType
import java.time.LocalDateTime

/**
 * 팀 일정 서비스 (Inbound Port)
 *
 * 팀 자체 일정(연습, 이벤트, 모임 등) CRUD 유스케이스를 정의합니다.
 * 구현체는 nextup-infrastructure의 TeamScheduleServiceImpl입니다.
 */
interface TeamScheduleService {
    /**
     * 팀 일정을 생성합니다.
     */
    fun create(
        teamId: Long,
        title: String,
        description: String?,
        scheduleType: TeamScheduleType,
        startAt: LocalDateTime,
        endAt: LocalDateTime?,
        location: String?,
    ): TeamSchedule

    /**
     * 팀 일정 목록을 조회합니다.
     */
    fun getByTeamId(teamId: Long): List<TeamSchedule>

    /**
     * 팀 일정 목록을 기간으로 필터링하여 조회합니다.
     */
    fun getByTeamIdAndDateRange(
        teamId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<TeamSchedule>

    /**
     * ID로 팀 일정을 조회합니다.
     */
    fun getById(id: Long): TeamSchedule

    /**
     * 팀 일정을 수정합니다.
     */
    fun update(
        id: Long,
        title: String? = null,
        description: String? = null,
        scheduleType: TeamScheduleType? = null,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
        location: String? = null,
    ): TeamSchedule

    /**
     * 팀 일정을 삭제합니다.
     */
    fun delete(id: Long)
}
