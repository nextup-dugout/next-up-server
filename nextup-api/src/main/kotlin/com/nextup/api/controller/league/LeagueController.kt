package com.nextup.api.controller.league

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.league.LeagueResponse
import com.nextup.api.dto.league.LeagueSummaryResponse
import com.nextup.core.service.league.LeagueService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 리그 조회 API Controller (일반 사용자용)
 */
@RestController
@RequestMapping("/api")
class LeagueController(
    private val leagueService: LeagueService,
) {
    /**
     * 활성화된 리그 목록을 조회합니다.
     */
    @GetMapping("/leagues")
    fun getLeagues(): ApiResponse<List<LeagueSummaryResponse>> {
        val leagues = leagueService.getAllActive()
        return ApiResponse.success(
            leagues.map { LeagueSummaryResponse.from(it) },
        )
    }

    /**
     * 리그 상세 정보를 조회합니다.
     */
    @GetMapping("/leagues/{id}")
    fun getLeague(
        @PathVariable id: Long,
    ): ApiResponse<LeagueResponse> {
        val league = leagueService.getById(id)
        return ApiResponse.success(LeagueResponse.from(league))
    }

    /**
     * 협회별 활성화된 리그 목록을 조회합니다.
     */
    @GetMapping("/associations/{associationId}/leagues")
    fun getLeaguesByAssociation(
        @PathVariable associationId: Long,
    ): ApiResponse<List<LeagueSummaryResponse>> {
        val leagues = leagueService.getActiveByAssociationId(associationId)
        return ApiResponse.success(
            leagues.map { LeagueSummaryResponse.from(it) },
        )
    }
}
