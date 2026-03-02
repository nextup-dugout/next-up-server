package com.nextup.api.controller.game

import com.nextup.api.dto.game.GameAggregateResponse
import com.nextup.api.mapper.game.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.GameAggregateService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 경기 상세 통합 API Controller
 *
 * GET /api/v1/games/{gameId}/aggregate
 *   - 경기 기본 정보, 박스스코어, 타임라인, 공식 기록지를 한 번에 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/games")
class GameAggregateController(
    private val gameAggregateService: GameAggregateService,
) {
    /**
     * 경기 상세 통합 데이터를 조회합니다.
     *
     * 프론트엔드 경기 상세 화면에서 필요한 모든 데이터를 단일 API로 제공합니다.
     * 박스스코어, 공식 기록지는 출전 선수가 없으면 null로 반환됩니다.
     *
     * @param gameId 경기 ID
     * @return 경기 상세 통합 응답
     */
    @GetMapping("/{gameId}/aggregate")
    fun getGameAggregate(
        @PathVariable gameId: Long,
    ): ApiResponse<GameAggregateResponse> {
        val aggregate = gameAggregateService.getGameAggregate(gameId)
        return ApiResponse.success(aggregate.toResponse())
    }
}
