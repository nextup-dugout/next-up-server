package com.nextup.api.controller.player

import com.nextup.api.dto.player.PlayerDashboardResponse
import com.nextup.api.mapper.player.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.player.PlayerDashboardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 대시보드 통합 API Controller
 *
 * GET /api/v1/players/{playerId}/dashboard
 *   - 선수 프로필, 현재 팀, 시즌/통산 타격/투수 통계, 최근 폼, 팀 이력을 한 번에 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/players")
class PlayerDashboardController(
    private val playerDashboardService: PlayerDashboardService,
) {
    /**
     * 선수 대시보드 데이터를 조회합니다.
     *
     * 프론트엔드 선수 프로필 화면에서 필요한 모든 데이터를 단일 API로 제공합니다.
     * 데이터가 없는 항목(통계, 폼 등)은 null로 반환됩니다.
     *
     * @param playerId 선수 ID
     * @return 선수 대시보드 통합 응답
     */
    @GetMapping("/{playerId}/dashboard")
    fun getPlayerDashboard(
        @PathVariable playerId: Long,
    ): ApiResponse<PlayerDashboardResponse> {
        val dashboard = playerDashboardService.getPlayerDashboard(playerId)
        return ApiResponse.success(dashboard.toResponse())
    }
}
