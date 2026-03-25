package com.nextup.api.controller.attendance

import com.nextup.api.dto.attendance.GameParticipationRateResponse
import com.nextup.api.dto.attendance.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.attendance.ActivityService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * 팀 활동 점수 조회 컨트롤러 (자동 집계)
 *
 * game_players 기반 경기참여율을 자동으로 집계하여 반환합니다.
 * 기존 수동 PUT 엔드포인트는 삭제되었습니다.
 */
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/v1/teams/{teamId}/activity")
class TeamActivityController(
    private val activityService: ActivityService,
) {
    /**
     * 팀의 모든 멤버별 경기참여율을 조회합니다.
     */
    @GetMapping
    fun listParticipationRates(
        @PathVariable teamId: Long,
    ): ApiResponse<List<GameParticipationRateResponse>> {
        val rates = activityService.listGameParticipationRates(teamId)
        return ApiResponse.success(rates.map { it.toResponse() })
    }

    /**
     * 특정 선수의 경기참여율을 조회합니다.
     */
    @GetMapping("/players/{playerId}")
    fun getPlayerParticipationRate(
        @PathVariable teamId: Long,
        @PathVariable playerId: Long,
    ): ApiResponse<GameParticipationRateResponse> {
        val rate = activityService.getGameParticipationRate(teamId, playerId)
        return ApiResponse.success(
            GameParticipationRateResponse(
                playerId = playerId,
                playerName = "",
                gamesPlayed = 0,
                totalTeamGames = 0,
                participationRate = rate,
            ),
        )
    }
}
