package com.nextup.api.controller.stats

import com.nextup.api.dto.stats.SeasonAwardResponse
import com.nextup.api.mapper.stats.toSeasonAwardResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.stats.SeasonAwardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 시즌 타이틀(개인상) 조회 API
 *
 * 대회별, 연도별, 선수별 시상 목록을 조회합니다.
 */
@RestController
@RequestMapping("/api/v1")
class SeasonAwardController(
    private val seasonAwardService: SeasonAwardService,
) {
    /**
     * 대회별 시상 목록 조회
     *
     * GET /api/v1/competitions/{competitionId}/awards
     */
    @GetMapping("/competitions/{competitionId}/awards")
    fun getAwardsByCompetition(
        @PathVariable competitionId: Long,
    ): ApiResponse<List<SeasonAwardResponse>> =
        ApiResponse.success(
            seasonAwardService.getAwardsByCompetitionId(competitionId).toSeasonAwardResponse(),
        )

    /**
     * 연도별 시상 목록 조회
     *
     * GET /api/v1/awards?year=2026
     */
    @GetMapping("/awards")
    fun getAwardsByYear(
        @RequestParam year: Int,
    ): ApiResponse<List<SeasonAwardResponse>> =
        ApiResponse.success(
            seasonAwardService.getAwardsByYear(year).toSeasonAwardResponse(),
        )

    /**
     * 선수별 시상 목록 조회
     *
     * GET /api/v1/players/{playerId}/awards
     */
    @GetMapping("/players/{playerId}/awards")
    fun getAwardsByPlayer(
        @PathVariable playerId: Long,
    ): ApiResponse<List<SeasonAwardResponse>> =
        ApiResponse.success(
            seasonAwardService.getAwardsByPlayerId(playerId).toSeasonAwardResponse(),
        )
}
