package com.nextup.api.controller

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.pitch.BallCountResponse
import com.nextup.api.dto.pitch.PitchEventResponse
import com.nextup.api.dto.pitch.PitcherStatsResponse
import com.nextup.api.dto.pitch.toResponse
import com.nextup.core.service.PitchEventService
import org.springframework.web.bind.annotation.*

/**
 * 투구 이벤트 조회 Controller (Public API)
 *
 * 일반 사용자가 투구 이벤트를 조회하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/games/{gameId}")
class PitchEventController(
    private val pitchEventService: PitchEventService,
) {
    /**
     * 경기의 모든 투구 이벤트를 조회합니다.
     */
    @GetMapping("/pitch-events")
    fun getGamePitchEvents(
        @PathVariable gameId: Long,
    ): ApiResponse<List<PitchEventResponse>> {
        val pitchEvents = pitchEventService.getGamePitchEvents(gameId)
        return ApiResponse.success(pitchEvents.toResponse())
    }

    /**
     * 경기의 현재 볼카운트를 조회합니다.
     */
    @GetMapping("/ball-count")
    fun getCurrentBallCount(
        @PathVariable gameId: Long,
    ): ApiResponse<BallCountResponse> {
        val (balls, strikes) = pitchEventService.getCurrentBallCount(gameId)
        return ApiResponse.success(BallCountResponse.of(balls, strikes))
    }

    /**
     * 특정 이닝의 투구 이벤트를 조회합니다.
     */
    @GetMapping("/pitch-events/inning/{inning}")
    fun getInningPitchEvents(
        @PathVariable gameId: Long,
        @PathVariable inning: Int,
        @RequestParam(required = true) isTopInning: Boolean,
    ): ApiResponse<List<PitchEventResponse>> {
        val pitchEvents =
            pitchEventService.getInningPitchEvents(
                gameId = gameId,
                inning = inning,
                isTopInning = isTopInning,
            )
        return ApiResponse.success(pitchEvents.toResponse())
    }
}

/**
 * 투수 투구 통계 Controller (Public API)
 */
@RestController
@RequestMapping("/api/v1/game-players/{gamePlayerId}/pitch-stats")
class PitcherStatsController(
    private val pitchEventService: PitchEventService,
) {
    /**
     * 투수의 투구 통계를 조회합니다.
     */
    @GetMapping
    fun getPitcherStats(
        @PathVariable gamePlayerId: Long,
    ): ApiResponse<PitcherStatsResponse> {
        val stats = pitchEventService.calculatePitcherStats(gamePlayerId)
        return ApiResponse.success(stats.toResponse())
    }
}
