package com.nextup.api.controller.stats

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.stats.TeamStatsResponse
import com.nextup.api.mapper.stats.toResponse
import com.nextup.core.service.stats.TeamStatsService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 팀 통계 API 컨트롤러
 *
 * 팀의 경기 성적, 타격/투수 통계를 제공합니다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/teams/{teamId}/stats")
class TeamStatsController(
    private val teamStatsService: TeamStatsService,
) {
    /**
     * 팀 통계를 조회합니다.
     *
     * @param teamId 팀 ID
     * @param year 시즌 연도 (선택, 1900-2100)
     * @param competitionId 대회 ID (선택)
     * @return 팀 통계 응답
     */
    @GetMapping
    fun getTeamStats(
        @PathVariable teamId: Long,
        @RequestParam(required = false) @Min(1900) @Max(2100) year: Int?,
        @RequestParam(required = false) competitionId: Long?,
    ): ApiResponse<TeamStatsResponse> {
        val stats = teamStatsService.getTeamStats(teamId, year, competitionId)
        return ApiResponse.success(stats.toResponse())
    }
}
