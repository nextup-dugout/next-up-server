package com.nextup.api.controller.game

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.game.GameTimelineResponse
import com.nextup.api.mapper.game.toResponse
import com.nextup.core.service.game.GameTimelineService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 경기 타임라인 API 컨트롤러
 *
 * 경기의 이벤트 타임라인(문자 중계 다시보기)을 제공합니다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/games/{gameId}/timeline")
class GameTimelineController(
    private val gameTimelineService: GameTimelineService,
) {
    /**
     * 경기의 타임라인을 조회합니다.
     *
     * @param gameId 경기 ID
     * @param fromInning 시작 이닝 (선택, 1-99)
     * @param toInning 종료 이닝 (선택, 1-99)
     * @return 경기 타임라인 응답
     */
    @GetMapping
    fun getTimeline(
        @PathVariable gameId: Long,
        @RequestParam(required = false) @Min(1) @Max(99) fromInning: Int?,
        @RequestParam(required = false) @Min(1) @Max(99) toInning: Int?,
    ): ApiResponse<GameTimelineResponse> {
        val timeline = gameTimelineService.getTimeline(gameId, fromInning, toInning)
        return ApiResponse.success(timeline.toResponse())
    }
}
