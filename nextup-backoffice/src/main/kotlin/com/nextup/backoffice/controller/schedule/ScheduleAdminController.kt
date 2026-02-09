package com.nextup.backoffice.controller.schedule

import com.nextup.backoffice.dto.common.ApiResponse
import com.nextup.backoffice.dto.schedule.CreateScheduleRequest
import com.nextup.backoffice.dto.schedule.ScheduleAdminResponse
import com.nextup.backoffice.dto.schedule.UpdateScheduleRequest
import com.nextup.core.service.schedule.LeagueScheduleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 대진표 관리 API Controller (관리자용)
 *
 * 전체 권한: 생성, 조회, 수정, 삭제
 */
@RestController
@RequestMapping("/api/backoffice/competitions/{competitionId}/schedule")
class ScheduleAdminController(
    private val scheduleService: LeagueScheduleService,
) {
    /**
     * 대회의 전체 대진표를 조회합니다.
     */
    @GetMapping
    fun getSchedules(
        @PathVariable competitionId: Long,
    ): ApiResponse<List<ScheduleAdminResponse>> {
        val schedules = scheduleService.getSchedulesByCompetition(competitionId)
        return ApiResponse.success(schedules.map { ScheduleAdminResponse.from(it) })
    }

    /**
     * 대진표를 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSchedule(
        @PathVariable competitionId: Long,
        @Valid @RequestBody request: CreateScheduleRequest,
    ): ApiResponse<ScheduleAdminResponse> {
        val schedule =
            scheduleService.createSchedule(
                competitionId = competitionId,
                round = request.round,
                matchNumber = request.matchNumber,
                homeTeamId = request.homeTeamId,
                awayTeamId = request.awayTeamId,
                scheduledDate = request.scheduledDate,
                scheduledTime = request.scheduledTime,
                venue = request.venue,
            )
        return ApiResponse.success(ScheduleAdminResponse.from(schedule))
    }

    /**
     * 대진표를 수정합니다.
     */
    @PutMapping("/{id}")
    fun updateSchedule(
        @PathVariable competitionId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateScheduleRequest,
    ): ApiResponse<ScheduleAdminResponse> {
        val schedule =
            scheduleService.updateSchedule(
                scheduleId = id,
                scheduledDate = request.scheduledDate,
                scheduledTime = request.scheduledTime,
                venue = request.venue,
            )
        return ApiResponse.success(ScheduleAdminResponse.from(schedule))
    }

    /**
     * 대진표를 삭제합니다.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSchedule(
        @PathVariable competitionId: Long,
        @PathVariable id: Long,
    ) {
        scheduleService.deleteSchedule(id)
    }
}
