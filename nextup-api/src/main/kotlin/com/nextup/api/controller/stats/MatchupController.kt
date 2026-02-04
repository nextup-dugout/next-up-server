package com.nextup.api.controller.stats

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.stats.MatchupResponse
import com.nextup.api.mapper.stats.toResponse
import com.nextup.core.service.stats.MatchupService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 투수 vs 타자 매치업 통계 API
 *
 * 투수와 타자의 대결 기록을 조회합니다.
 * 모든 응답은 ApiResponse로 래핑됩니다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/matchups")
class MatchupController(
    private val matchupService: MatchupService,
) {
    /**
     * 투수 vs 타자 매치업 기록 조회
     *
     * GET /api/v1/matchups/pitcher/{pitcherId}/batter/{batterId}
     *
     * @param pitcherId 투수 ID
     * @param batterId 타자 ID
     * @param year 시즌 연도 (선택)
     * @param competitionId 대회 ID (선택, 추후 구현)
     * @return 매치업 통계 및 히스토리
     */
    @GetMapping("/pitcher/{pitcherId}/batter/{batterId}")
    fun getMatchup(
        @PathVariable pitcherId: Long,
        @PathVariable batterId: Long,
        @RequestParam(required = false) @Min(1900) @Max(2100) year: Int?,
        @RequestParam(required = false) competitionId: Long?,
    ): ApiResponse<MatchupResponse> {
        val matchup = matchupService.getMatchup(pitcherId, batterId, year, competitionId)
        return ApiResponse.success(matchup.toResponse())
    }
}
