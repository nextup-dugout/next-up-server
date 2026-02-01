package com.nextup.api.controller.stats

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.stats.CareerBattingStatsResponse
import com.nextup.api.dto.stats.CareerPitchingStatsResponse
import com.nextup.api.dto.stats.SeasonBattingStatsResponse
import com.nextup.api.dto.stats.SeasonPitchingStatsResponse
import com.nextup.api.mapper.stats.toResponse
import com.nextup.api.mapper.stats.toSeasonBattingResponse
import com.nextup.api.mapper.stats.toSeasonPitchingResponse
import com.nextup.infrastructure.service.stats.PlayerStatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 통계 조회 API
 *
 * 선수별 시즌/통산 타격/투수 통계를 조회합니다.
 * 모든 응답은 ApiResponse로 래핑됩니다.
 */
@RestController
@RequestMapping("/api/v1/players/{playerId}/stats")
class PlayerStatsController(
    private val playerStatsService: PlayerStatsService
) {

    /**
     * 시즌 타격 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/batting/season/{year}
     *
     * @param playerId 선수 ID
     * @param year 시즌 연도
     * @return 시즌 타격 통계 응답
     */
    @GetMapping("/batting/season/{year}")
    fun getSeasonBattingStats(
        @PathVariable playerId: Long,
        @PathVariable year: Int
    ): ApiResponse<SeasonBattingStatsResponse> {
        val stats = playerStatsService.getSeasonBattingStats(playerId, year)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 시즌 투수 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/pitching/season/{year}
     *
     * @param playerId 선수 ID
     * @param year 시즌 연도
     * @return 시즌 투수 통계 응답
     */
    @GetMapping("/pitching/season/{year}")
    fun getSeasonPitchingStats(
        @PathVariable playerId: Long,
        @PathVariable year: Int
    ): ApiResponse<SeasonPitchingStatsResponse> {
        val stats = playerStatsService.getSeasonPitchingStats(playerId, year)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 통산 타격 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/batting/career
     *
     * @param playerId 선수 ID
     * @return 통산 타격 통계 응답
     */
    @GetMapping("/batting/career")
    fun getCareerBattingStats(
        @PathVariable playerId: Long
    ): ApiResponse<CareerBattingStatsResponse> {
        val stats = playerStatsService.getCareerBattingStats(playerId)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 통산 투수 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/pitching/career
     *
     * @param playerId 선수 ID
     * @return 통산 투수 통계 응답
     */
    @GetMapping("/pitching/career")
    fun getCareerPitchingStats(
        @PathVariable playerId: Long
    ): ApiResponse<CareerPitchingStatsResponse> {
        val stats = playerStatsService.getCareerPitchingStats(playerId)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 모든 시즌 타격 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/batting/seasons
     *
     * @param playerId 선수 ID
     * @return 모든 시즌 타격 통계 리스트
     */
    @GetMapping("/batting/seasons")
    fun getAllSeasonBattingStats(
        @PathVariable playerId: Long
    ): ApiResponse<List<SeasonBattingStatsResponse>> {
        val statsList = playerStatsService.getAllSeasonBattingStats(playerId)
        return ApiResponse.success(statsList.toSeasonBattingResponse())
    }

    /**
     * 모든 시즌 투수 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/pitching/seasons
     *
     * @param playerId 선수 ID
     * @return 모든 시즌 투수 통계 리스트
     */
    @GetMapping("/pitching/seasons")
    fun getAllSeasonPitchingStats(
        @PathVariable playerId: Long
    ): ApiResponse<List<SeasonPitchingStatsResponse>> {
        val statsList = playerStatsService.getAllSeasonPitchingStats(playerId)
        return ApiResponse.success(statsList.toSeasonPitchingResponse())
    }
}
