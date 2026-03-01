package com.nextup.api.controller.team

import com.nextup.api.dto.team.TeamDashboardResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.team.TeamDashboardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 팀 대시보드 API 컨트롤러
 *
 * 팀 홈 화면에 필요한 데이터(다음 경기, 최근 결과, 순위, 출석 현황 등)를 한 번에 제공합니다.
 *
 * GET /api/v1/teams/{teamId}/dashboard
 */
@RestController
@RequestMapping("/api/v1/teams/{teamId}/dashboard")
class TeamDashboardController(
    private val teamDashboardService: TeamDashboardService,
) {
    /**
     * 팀 대시보드 정보를 조회합니다.
     *
     * @param teamId 팀 ID
     * @return 팀 대시보드 통합 응답
     */
    @GetMapping
    fun getTeamDashboard(
        @PathVariable teamId: Long,
    ): ApiResponse<TeamDashboardResponse> {
        val dto = teamDashboardService.getTeamDashboard(teamId)
        return ApiResponse.success(TeamDashboardResponse.from(dto))
    }
}
