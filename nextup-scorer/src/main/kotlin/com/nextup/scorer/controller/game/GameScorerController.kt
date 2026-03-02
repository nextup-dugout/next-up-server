package com.nextup.scorer.controller.game

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.GameScorerService
import com.nextup.scorer.dto.game.ForfeitRequestDto
import com.nextup.scorer.dto.game.GameEndRequestDto
import com.nextup.scorer.dto.game.GameResponse
import com.nextup.scorer.dto.game.PlateAppearanceRequestDto
import com.nextup.scorer.dto.game.SubstitutionRequestDto
import com.nextup.scorer.dto.game.SubstitutionResponse
import com.nextup.scorer.dto.game.UndoResponse
import com.nextup.scorer.dto.game.toDomain
import com.nextup.scorer.dto.game.toResponse
import com.nextup.scorer.dto.game.toSubstitutionResponse
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
     *
     * 응답에 경고 메시지(투구 수 경고, 타순 위반 경고)가 포함될 수 있습니다.
     */
    @PostMapping("/{gameId}/plate-appearances")
    @ResponseStatus(HttpStatus.OK)
    fun recordPlateAppearance(
        @PathVariable gameId: Long,
        @RequestBody @Valid request: PlateAppearanceRequestDto
    ): ApiResponse<GameResponse> {
        val result = gameScorerService.recordPlateAppearance(gameId, request.toDomain())
        return ApiResponse.success(result.game.toResponse(warnings = result.warnings))
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

    /**
     * 마지막 이벤트를 되돌립니다 (Undo).
     */
    @PostMapping("/{gameId}/undo")
    @ResponseStatus(HttpStatus.OK)
    fun undoLastEvent(
        @PathVariable gameId: Long
    ): ApiResponse<UndoResponse> {
        val undoneEvent = gameScorerService.undoLastEvent(gameId)
        val game = undoneEvent.game
        return ApiResponse.success(
            UndoResponse(
                undoneEventId = undoneEvent.id,
                eventType = undoneEvent.eventType.displayName,
                restoredState = game.gameState.toResponse(),
                message = "${undoneEvent.eventType.displayName} 이벤트가 되돌려졌습니다.",
            )
        )
    }

    /**
     * 경기를 몰수 처리합니다.
     *
     * 승리팀에 7점, 패배팀에 0점을 자동 반영하고 경기를 종료합니다.
     */
    @PostMapping("/{gameId}/forfeit")
    @ResponseStatus(HttpStatus.OK)
    fun forfeitGame(
        @PathVariable gameId: Long,
        @RequestBody @Valid request: ForfeitRequestDto,
    ): ApiResponse<GameResponse> {
        val game =
            gameScorerService.forfeitGame(
                gameId = gameId,
                winnerTeamId = request.winnerTeamId!!,
                reason = request.reason!!,
            )
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 선수를 교체합니다.
     *
     * 교체 이벤트를 기록하고 다음을 검증합니다:
     * - 퇴장한 선수의 재출전 방지
     * - DH 해제 규칙 (투수가 DH 타순으로만 교체 가능)
     */
    @PostMapping("/{gameId}/substitutions")
    @ResponseStatus(HttpStatus.OK)
    fun substitutePlayer(
        @PathVariable gameId: Long,
        @RequestBody @Valid request: SubstitutionRequestDto,
    ): ApiResponse<SubstitutionResponse> {
        val event = gameScorerService.substitutePlayer(gameId, request.toDomain())
        return ApiResponse.success(event.toSubstitutionResponse())
    }
}
