package com.nextup.api.controller.team

import com.nextup.api.dto.team.TeamMatchupGameResponse
import com.nextup.api.dto.team.TeamMatchupResponse
import com.nextup.api.dto.team.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.team.TeamMatchupService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 팀 간 상대 전적 API
 *
 * 두 팀 간의 대결 전적 및 최근 경기 기록을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/teams")
class TeamMatchupController(
    private val teamMatchupService: TeamMatchupService,
) {
    /**
     * 두 팀 간의 상대 전적을 조회합니다.
     *
     * GET /api/v1/teams/{teamId}/matchup/{opponentId}
     *
     * @param teamId 조회 대상 팀 ID
     * @param opponentId 상대 팀 ID
     * @param competitionId 대회 ID 필터 (선택사항)
     * @return 팀 간 상대 전적 정보
     */
    @GetMapping("/{teamId}/matchup/{opponentId}")
    fun getTeamMatchup(
        @PathVariable teamId: Long,
        @PathVariable opponentId: Long,
        @RequestParam(required = false) competitionId: Long?,
    ): ApiResponse<TeamMatchupResponse> {
        val matchup =
            teamMatchupService.getTeamMatchup(
                teamId = teamId,
                opponentId = opponentId,
                competitionId = competitionId,
            )
        return ApiResponse.success(matchup.toResponse())
    }

    /**
     * 두 팀 간의 최근 경기 목록을 조회합니다.
     *
     * GET /api/v1/teams/{teamId}/matchup/{opponentId}/games
     *
     * @param teamId 조회 대상 팀 ID
     * @param opponentId 상대 팀 ID
     * @param limit 조회할 경기 수 (기본값: 10, 최대: 50)
     * @return 최근 교전 경기 목록
     */
    @GetMapping("/{teamId}/matchup/{opponentId}/games")
    fun getRecentGames(
        @PathVariable teamId: Long,
        @PathVariable opponentId: Long,
        @RequestParam(defaultValue = "10") limit: Int,
    ): ApiResponse<List<TeamMatchupGameResponse>> {
        val validatedLimit = limit.coerceIn(1, 50)

        val games =
            teamMatchupService.getRecentGames(
                teamId = teamId,
                opponentId = opponentId,
                limit = validatedLimit,
            )
        return ApiResponse.success(games.toResponse())
    }
}
