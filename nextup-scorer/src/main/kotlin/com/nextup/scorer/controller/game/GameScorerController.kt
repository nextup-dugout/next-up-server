package com.nextup.scorer.controller.game

import com.nextup.core.service.game.GameScorerService
import com.nextup.scorer.dto.common.ApiResponse
import com.nextup.scorer.dto.game.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 기록원 전용 경기 기록 컨트롤러
 *
 * 실시간 경기 기록 입력을 위한 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/scorer/games")
class GameScorerController(
    private val gameScorerService: GameScorerService
) {

    /**
     * 경기를 시작합니다.
     */
    @PostMapping("/{gameId}/start")
    @ResponseStatus(HttpStatus.OK)
    fun startGame(
        @PathVariable gameId: Long
    ): ApiResponse<GameResponse> {
        val game = gameScorerService.startGame(gameId)
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 타석 결과를 입력합니다.
     */
    @PostMapping("/{gameId}/plate-appearances")
    @ResponseStatus(HttpStatus.OK)
    fun recordPlateAppearance(
        @PathVariable gameId: Long,
        @RequestBody @Valid request: PlateAppearanceRequestDto
    ): ApiResponse<GameResponse> {
        val game = gameScorerService.recordPlateAppearance(gameId, request.toDomain())
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 반 이닝을 진행합니다 (공수 교대).
     */
    @PostMapping("/{gameId}/half-inning")
    @ResponseStatus(HttpStatus.OK)
    fun advanceHalfInning(
        @PathVariable gameId: Long
    ): ApiResponse<GameResponse> {
        val game = gameScorerService.advanceHalfInning(gameId)
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 경기를 종료합니다.
     */
    @PostMapping("/{gameId}/end")
    @ResponseStatus(HttpStatus.OK)
    fun endGame(
        @PathVariable gameId: Long,
        @RequestBody @Valid request: GameEndRequestDto
    ): ApiResponse<GameResponse> {
        val game = gameScorerService.endGame(gameId, request.reason!!)
        return ApiResponse.success(game.toResponse())
    }
}
