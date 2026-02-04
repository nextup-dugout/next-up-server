package com.nextup.api.controller.competition

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.competition.CompetitionResponse
import com.nextup.core.service.competition.CompetitionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 대회 API Controller (일반 사용자 - 조회 전용)
 */
@RestController
@RequestMapping("/api/v1/competitions")
class CompetitionController(
    private val competitionService: CompetitionService,
) {
    /**
     * 진행 중인 대회 목록을 조회합니다.
     */
    @GetMapping
    fun getCompetitions(): ApiResponse<List<CompetitionResponse>> {
        val competitions = competitionService.getInProgress()
        return ApiResponse.success(
            competitions.map { CompetitionResponse.from(it) },
        )
    }

    /**
     * 대회 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getCompetition(
        @PathVariable id: Long,
    ): ApiResponse<CompetitionResponse> {
        val competition = competitionService.getByIdWithLeague(id)
        return ApiResponse.success(CompetitionResponse.from(competition))
    }

    /**
     * 리그별 대회 목록을 조회합니다.
     */
    @GetMapping("/by-league/{leagueId}")
    fun getCompetitionsByLeague(
        @PathVariable leagueId: Long,
    ): ApiResponse<List<CompetitionResponse>> {
        val competitions = competitionService.getByLeagueId(leagueId)
        return ApiResponse.success(
            competitions.map { CompetitionResponse.from(it) },
        )
    }
}
