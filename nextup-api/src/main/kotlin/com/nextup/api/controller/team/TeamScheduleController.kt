package com.nextup.api.controller.team

import com.nextup.api.dto.team.CreateTeamScheduleRequest
import com.nextup.api.dto.team.TeamScheduleResponse
import com.nextup.api.dto.team.UpdateTeamScheduleRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.team.TeamScheduleService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 팀 일정 API Controller
 *
 * 경기 일정 외 연습/이벤트/모임 등 팀 자체 일정을 관리합니다.
 */
@RestController
@RequestMapping("/api/v1/teams/{teamId}/schedules")
class TeamScheduleController(
    private val teamScheduleService: TeamScheduleService,
) {
    /**
     * 팀 일정을 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSchedule(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: CreateTeamScheduleRequest,
    ): ApiResponse<TeamScheduleResponse> {
        val schedule =
            teamScheduleService.create(
                teamId = teamId,
                title = request.title,
                description = request.description,
                scheduleType = request.scheduleType,
                startAt = request.startAt,
                endAt = request.endAt,
                location = request.location,
            )
        return ApiResponse.success(TeamScheduleResponse.from(schedule))
    }

    /**
     * 팀 일정 목록을 조회합니다.
     *
     * @param from 시작 날짜 필터 (null이면 전체 조회)
     * @param to 종료 날짜 필터 (null이면 전체 조회)
     */
    @GetMapping
    fun getSchedules(
        @PathVariable teamId: Long,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,
    ): ApiResponse<List<TeamScheduleResponse>> {
        val schedules =
            if (from != null && to != null) {
                teamScheduleService.getByTeamIdAndDateRange(teamId, from, to)
            } else {
                teamScheduleService.getByTeamId(teamId)
            }
        return ApiResponse.success(schedules.map { TeamScheduleResponse.from(it) })
    }

    /**
     * 팀 일정 상세를 조회합니다.
     */
    @GetMapping("/{scheduleId}")
    fun getSchedule(
        @PathVariable teamId: Long,
        @PathVariable scheduleId: Long,
    ): ApiResponse<TeamScheduleResponse> {
        val schedule = teamScheduleService.getById(scheduleId)
        return ApiResponse.success(TeamScheduleResponse.from(schedule))
    }

    /**
     * 팀 일정을 수정합니다.
     */
    @PatchMapping("/{scheduleId}")
    fun updateSchedule(
        @PathVariable teamId: Long,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: UpdateTeamScheduleRequest,
    ): ApiResponse<TeamScheduleResponse> {
        val schedule =
            teamScheduleService.update(
                id = scheduleId,
                title = request.title,
                description = request.description,
                scheduleType = request.scheduleType,
                startAt = request.startAt,
                endAt = request.endAt,
                location = request.location,
            )
        return ApiResponse.success(TeamScheduleResponse.from(schedule))
    }

    /**
     * 팀 일정을 삭제합니다.
     */
    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSchedule(
        @PathVariable teamId: Long,
        @PathVariable scheduleId: Long,
    ): ApiResponse<Unit> {
        teamScheduleService.delete(scheduleId)
        return ApiResponse.success(Unit)
    }
}
