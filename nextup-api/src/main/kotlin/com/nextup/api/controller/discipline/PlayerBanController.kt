package com.nextup.api.controller.discipline

import com.nextup.api.dto.discipline.PlayerBanHistoryResponse
import com.nextup.api.dto.discipline.PlayerBanResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.discipline.PlayerBanService
import org.springframework.web.bind.annotation.*

/**
 * 선수 제재 조회 API Controller (공개 API)
 *
 * 조회 전용
 */
@RestController
@RequestMapping("/api/v1/player-bans")
class PlayerBanController(
    private val playerBanService: PlayerBanService,
) {
    /**
     * 선수의 제재 이력을 조회합니다.
     */
    @GetMapping("/players/{playerId}")
    fun getPlayerBans(
        @PathVariable playerId: Long,
        @RequestParam(required = false) competitionId: Long?,
    ): ApiResponse<PlayerBanHistoryResponse> {
        val bans =
            if (competitionId != null) {
                playerBanService.getBansByPlayerAndCompetition(playerId, competitionId)
            } else {
                playerBanService.getBansByPlayer(playerId)
            }

        val response =
            PlayerBanHistoryResponse(
                playerId = playerId,
                totalBans = bans.size,
                bans = bans.map { PlayerBanResponse.from(it) },
            )

        return ApiResponse.success(response)
    }

    /**
     * 선수가 출장 가능한지 확인합니다.
     */
    @GetMapping("/players/{playerId}/eligibility")
    fun checkPlayerEligibility(
        @PathVariable playerId: Long,
        @RequestParam competitionId: Long,
    ): ApiResponse<Map<String, Any>> {
        val canPlay = playerBanService.canPlayerPlay(playerId, competitionId)
        val bans =
            playerBanService.getBansByPlayerAndCompetition(playerId, competitionId)

        return ApiResponse.success(
            mapOf(
                "canPlay" to canPlay,
                "bansCount" to bans.size,
                "bans" to bans.map { PlayerBanResponse.from(it) },
            ),
        )
    }
}
