package com.nextup.api.controller.stats

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.stats.BattingLeaderResponse
import com.nextup.api.dto.stats.PitchingLeaderResponse
import com.nextup.api.mapper.stats.toBattingLeaderResponse
import com.nextup.api.mapper.stats.toPitchingLeaderResponse
import com.nextup.core.service.stats.IndividualRankingService
import com.nextup.core.service.stats.dto.BattingCategory
import com.nextup.core.service.stats.dto.PitchingCategory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 개인 타이틀 리더보드 API
 *
 * 대회별 카테고리 TOP N 리더보드를 조회합니다.
 */
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/leaderboard")
class LeaderboardController(
    private val individualRankingService: IndividualRankingService,
) {
    /**
     * 타격 리더보드 조회
     *
     * GET /api/v1/competitions/{competitionId}/leaderboard/batting?category=BATTING_AVG
     */
    @GetMapping("/batting")
    fun getBattingLeaders(
        @PathVariable competitionId: Long,
        @RequestParam category: BattingCategory,
        @RequestParam(defaultValue = "10") limit: Int,
    ): ApiResponse<List<BattingLeaderResponse>> {
        val leaders = individualRankingService.getBattingLeaders(competitionId, category, limit)
        return ApiResponse.success(leaders.toBattingLeaderResponse())
    }

    /**
     * 투수 리더보드 조회
     *
     * GET /api/v1/competitions/{competitionId}/leaderboard/pitching?category=ERA
     */
    @GetMapping("/pitching")
    fun getPitchingLeaders(
        @PathVariable competitionId: Long,
        @RequestParam category: PitchingCategory,
        @RequestParam(defaultValue = "10") limit: Int,
    ): ApiResponse<List<PitchingLeaderResponse>> {
        val leaders = individualRankingService.getPitchingLeaders(competitionId, category, limit)
        return ApiResponse.success(leaders.toPitchingLeaderResponse())
    }
}
