package com.nextup.api.controller.stats

import com.nextup.api.dto.stats.CareerFieldingStatsResponse
import com.nextup.api.dto.stats.SeasonFieldingStatsResponse
import com.nextup.api.mapper.stats.toResponse
import com.nextup.api.mapper.stats.toSeasonFieldingResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.stats.PlayerFieldingStatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 수비 통계 조회 API
 *
 * 선수별 시즌/통산 수비 통계를 조회합니다.
 * 모든 응답은 ApiResponse로 래핑됩니다.
 */
@RestController
@RequestMapping("/api/v1/players/{playerId}/stats")
class PlayerFieldingStatsController(
    private val playerFieldingStatsService: PlayerFieldingStatsService,
) {
    /**
     * 시즌 수비 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/fielding/season/{year}
     *
     * @param playerId 선수 ID
     * @param year 시즌 연도
     * @return 시즌 수비 통계 응답
     */
    @GetMapping("/fielding/season/{year}")
    fun getSeasonFieldingStats(
        @PathVariable playerId: Long,
        @PathVariable year: Int,
    ): ApiResponse<SeasonFieldingStatsResponse> {
        val stats = playerFieldingStatsService.getSeasonFieldingStats(playerId, year)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 통산 수비 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/fielding/career
     *
     * @param playerId 선수 ID
     * @return 통산 수비 통계 응답
     */
    @GetMapping("/fielding/career")
    fun getCareerFieldingStats(
        @PathVariable playerId: Long,
    ): ApiResponse<CareerFieldingStatsResponse> {
        val stats = playerFieldingStatsService.getCareerFieldingStats(playerId)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 모든 시즌 수비 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/fielding/seasons
     *
     * @param playerId 선수 ID
     * @return 모든 시즌 수비 통계 리스트
     */
    @GetMapping("/fielding/seasons")
    fun getAllSeasonFieldingStats(
        @PathVariable playerId: Long,
    ): ApiResponse<List<SeasonFieldingStatsResponse>> {
        val statsList = playerFieldingStatsService.getAllSeasonFieldingStats(playerId)
        return ApiResponse.success(statsList.toSeasonFieldingResponse())
    }
}
