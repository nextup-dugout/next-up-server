package com.nextup.api.controller.player

import com.nextup.api.dto.player.GameLogResponse
import com.nextup.api.mapper.player.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.GameLogService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 게임 로그 Controller (일반 사용자용)
 *
 * 선수의 최근 경기별 기록을 조회하는 API를 제공합니다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/players")
class GameLogController(
    private val gameLogService: GameLogService,
) {
    /**
     * 선수의 최근 경기별 기록(게임 로그)을 조회합니다.
     *
     * GET /api/v1/players/{playerId}/game-log
     *
     * @param playerId 선수 ID
     * @param limit 조회할 경기 수 (기본값: 10, 최대: 50)
     * @return 경기별 기록 리스트 (최근 경기 순)
     */
    @GetMapping("/{playerId}/game-log")
    fun getGameLog(
        @PathVariable playerId: Long,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) limit: Int,
    ): ApiResponse<List<GameLogResponse>> {
        val entries = gameLogService.getGameLog(playerId, limit)
        return ApiResponse.success(entries.toResponse())
    }
}
