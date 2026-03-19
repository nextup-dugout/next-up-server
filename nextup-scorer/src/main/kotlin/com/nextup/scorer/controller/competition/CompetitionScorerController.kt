package com.nextup.scorer.controller.competition

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.competition.CompetitionService
import com.nextup.scorer.dto.competition.CompetitionScorerResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 대회 조회 API Controller (기록원용)
 *
 * 기록원은 경기 기록 시 소속 대회를 조회할 수 있습니다.
 * 대회 생성·수정·삭제 및 상태 변경은 관리자(backoffice)의 책임입니다.
 */
@RestController
@RequestMapping("/api/v1/scorer/competitions")
class CompetitionScorerController(
    private val competitionService: CompetitionService
) {

    /**
     * 모든 대회 목록을 조회합니다.
     */
    @GetMapping
    fun getAllCompetitions(): ApiResponse<List<CompetitionScorerResponse>> {
        val competitions = competitionService.getAll()
        return ApiResponse.success(
            competitions.map { CompetitionScorerResponse.from(it) }
        )
    }

    /**
     * 대회 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getCompetition(
        @PathVariable id: Long
    ): ApiResponse<CompetitionScorerResponse> {
        val competition = competitionService.getByIdWithLeague(id)
        return ApiResponse.success(CompetitionScorerResponse.from(competition))
    }

    /**
     * 리그별 대회 목록을 조회합니다.
     */
    @GetMapping("/by-league/{leagueId}")
    fun getCompetitionsByLeague(
        @PathVariable leagueId: Long
    ): ApiResponse<List<CompetitionScorerResponse>> {
        val competitions = competitionService.getByLeagueId(leagueId)
        return ApiResponse.success(
            competitions.map { CompetitionScorerResponse.from(it) }
        )
    }
}
