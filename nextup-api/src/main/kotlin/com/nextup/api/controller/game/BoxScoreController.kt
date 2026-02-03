package com.nextup.api.controller.game

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.game.BoxScoreResponse
import com.nextup.api.mapper.game.toResponse
import com.nextup.core.service.game.BoxScoreService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 박스스코어 API 컨트롤러
 */
@RestController
@RequestMapping("/api/games/{gameId}/boxscore")
class BoxScoreController(
    private val boxScoreService: BoxScoreService
) {

    /**
     * 경기의 박스스코어를 조회합니다.
     */
    @GetMapping
    fun getBoxScore(@PathVariable gameId: Long): ApiResponse<BoxScoreResponse> {
        val boxScore = boxScoreService.getBoxScore(gameId)
        return ApiResponse.success(boxScore.toResponse())
    }
}
