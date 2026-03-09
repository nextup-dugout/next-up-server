package com.nextup.backoffice.controller.schedule

import com.nextup.backoffice.dto.schedule.CreateGameFromScheduleRequest
import com.nextup.backoffice.dto.schedule.CreateScheduleRequest
import com.nextup.backoffice.dto.schedule.PostponeBulkRequest
import com.nextup.backoffice.dto.schedule.RescheduleRequest
import com.nextup.backoffice.dto.schedule.ScheduleAdminResponse
import com.nextup.backoffice.dto.schedule.ScheduleConflictResponse
import com.nextup.backoffice.dto.schedule.ScheduleGenerateRequest
import com.nextup.backoffice.dto.schedule.UpdateScheduleRequest
import com.nextup.common.dto.ApiResponse
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

    /**
     * 대진표를 검증합니다. (Dry-run)
     *
     * 실제로 생성하지 않고 충돌만 확인합니다.
     */
    @PostMapping("/validate")
    fun validateSchedule(
        @PathVariable competitionId: Long,
        @Valid @RequestBody request: CreateScheduleRequest,
    ): ApiResponse<List<ScheduleConflictResponse>> {
        val conflicts =
            scheduleService.validateSchedule(
                competitionId = competitionId,
                round = request.round,
                matchNumber = request.matchNumber,
                homeTeamId = request.homeTeamId,
                awayTeamId = request.awayTeamId,
                scheduledDate = request.scheduledDate,
                scheduledTime = request.scheduledTime,
                venue = request.venue,
            )
        return ApiResponse.success(conflicts.map { ScheduleConflictResponse.from(it) })
    }

    /**
     * 라운드 로빈 방식으로 대진표를 자동 생성합니다.
     */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateSchedule(
        @PathVariable competitionId: Long,
        @Valid @RequestBody request: ScheduleGenerateRequest,
    ): ApiResponse<List<ScheduleAdminResponse>> {
        val schedules =
            scheduleService.generateRoundRobinSchedule(
                competitionId = competitionId,
                teamIds = request.teamIds,
                doubleRoundRobin = request.doubleRoundRobin,
            )
        return ApiResponse.success(schedules.map { ScheduleAdminResponse.from(it) })
    }

    /**
     * 특정 날짜의 경기를 일괄 연기합니다. (우천 순연)
     */
    @PostMapping("/postpone-bulk")
    fun postponeGamesBulk(
        @PathVariable competitionId: Long,
        @Valid @RequestBody request: PostponeBulkRequest,
    ): ApiResponse<List<ScheduleAdminResponse>> {
        val schedules =
            scheduleService.postponeGamesBulk(
                competitionId = competitionId,
                date = request.date,
                reason = request.reason,
            )
        return ApiResponse.success(schedules.map { ScheduleAdminResponse.from(it) })
    }

    /**
     * 대진표에서 경기를 생성합니다.
     *
     * 대진표의 대회/홈팀/원정팀/일정 정보를 기반으로 Game을 생성하고 연결합니다.
     */
    @PostMapping("/{id}/create-game")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGameFromSchedule(
        @PathVariable competitionId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: CreateGameFromScheduleRequest?,
    ): ApiResponse<ScheduleAdminResponse> {
        val schedule =
            scheduleService.createGameFromSchedule(
                scheduleId = id,
                location = request?.location,
                fieldName = request?.fieldName,
            )
        return ApiResponse.success(ScheduleAdminResponse.from(schedule))
    }

    /**
     * 연기된 경기의 일정을 재조정합니다.
     */
    @PutMapping("/{id}/reschedule")
    fun rescheduleGame(
        @PathVariable competitionId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: RescheduleRequest,
    ): ApiResponse<ScheduleAdminResponse> {
        val schedule =
            scheduleService.rescheduleGame(
                scheduleId = id,
                newDate = request.newDate,
                newVenue = request.newVenue,
            )
        return ApiResponse.success(ScheduleAdminResponse.from(schedule))
    }
}
