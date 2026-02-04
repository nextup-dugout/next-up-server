package com.nextup.api.controller.stats

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.stats.TeamStatsResponse
import com.nextup.api.mapper.stats.toResponse
import com.nextup.core.service.stats.TeamStatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/teams/{teamId}/stats")
class TeamStatsController(
    private val teamStatsService: TeamStatsService,
) {
    @GetMapping
    fun getTeamStats(
        @PathVariable teamId: Long,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) competitionId: Long?,
    ): ApiResponse<TeamStatsResponse> {
        val stats = teamStatsService.getTeamStats(teamId, year, competitionId)
        return ApiResponse.success(stats.toResponse())
    }
}
