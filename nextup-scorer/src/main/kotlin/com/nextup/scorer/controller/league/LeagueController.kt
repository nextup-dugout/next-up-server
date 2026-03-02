package com.nextup.scorer.controller.league

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.league.LeagueService
import com.nextup.scorer.dto.league.LeagueResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 리그 조회 API Controller (기록원용)
 *
 * 기록원은 경기 기록 시 소속 대회/리그를 조회할 수 있습니다.
 * 리그 생성·수정·삭제는 관리자(backoffice)의 책임입니다.
 */
@RestController
@RequestMapping("/api/scorer/leagues")
class LeagueController(
    private val leagueService: LeagueService
) {

    /**
     * 모든 리그 목록을 조회합니다 (비활성화 포함).
     * TODO: 기록원 본인 협회 필터링 적용
     */
    @GetMapping
    fun getAllLeagues(
        @RequestParam(required = false) associationId: Long?
    ): ApiResponse<List<LeagueResponse>> {
        val leagues =
            if (associationId != null) {
                leagueService.getByAssociationId(associationId)
            } else {
                leagueService.getAll()
            }

        return ApiResponse.success(
            leagues.map { LeagueResponse.from(it) }
        )
    }

    /**
     * 리그 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getLeague(
        @PathVariable id: Long
    ): ApiResponse<LeagueResponse> {
        val league = leagueService.getById(id)
        return ApiResponse.success(LeagueResponse.from(league))
    }
}
