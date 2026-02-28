package com.nextup.api.controller.schedule

import com.nextup.api.dto.schedule.ScheduleResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.schedule.LeagueScheduleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 대진표 조회 API Controller (일반 사용자용)
 *
 * 대회별 대진표 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/schedule")
class ScheduleController(
    private val scheduleService: LeagueScheduleService,
) {
    /**
     * 대회의 전체 대진표를 조회합니다.
     */
    @GetMapping
    fun getSchedules(
        @PathVariable competitionId: Long,
    ): ApiResponse<List<ScheduleResponse>> {
        val schedules = scheduleService.getSchedulesByCompetition(competitionId)
        return ApiResponse.success(schedules.map { ScheduleResponse.from(it) })
    }

    /**
     * 대회의 라운드별 대진표를 조회합니다.
     */
    @GetMapping("/round/{round}")
    fun getSchedulesByRound(
        @PathVariable competitionId: Long,
        @PathVariable round: Int,
    ): ApiResponse<List<ScheduleResponse>> {
        val schedules = scheduleService.getSchedulesByRound(competitionId, round)
        return ApiResponse.success(schedules.map { ScheduleResponse.from(it) })
    }
}
