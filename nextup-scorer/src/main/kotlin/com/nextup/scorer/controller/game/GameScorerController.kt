package com.nextup.scorer.controller.game

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.BaseRunningRecordService
import com.nextup.core.service.game.GameLifecycleService
import com.nextup.core.service.game.GameStateQueryService
import com.nextup.core.service.game.GameSubstitutionService
import com.nextup.core.service.game.GameTimelineService
import com.nextup.core.service.game.GameUndoService
import com.nextup.core.service.game.PlateAppearanceRecordService
import com.nextup.scorer.dto.game.BaseRunningRequestDto
import com.nextup.scorer.dto.game.BaseRunningResponse
import com.nextup.scorer.dto.game.CancelGameRequestDto
import com.nextup.scorer.dto.game.CurrentGameStateResponse
import com.nextup.scorer.dto.game.CurrentLineupResponse
import com.nextup.scorer.dto.game.EventTimelineResponse
import com.nextup.scorer.dto.game.ForfeitRequestDto
import com.nextup.scorer.dto.game.GameEndRequestDto
import com.nextup.scorer.dto.game.GameResponse
import com.nextup.scorer.dto.game.PlateAppearanceRequestDto
import com.nextup.scorer.dto.game.ScoreboardResponse
import com.nextup.scorer.dto.game.SubstitutionRequestDto
import com.nextup.scorer.dto.game.SubstitutionResponse
import com.nextup.scorer.dto.game.SuspendGameRequestDto
import com.nextup.scorer.dto.game.UndoResponse
import com.nextup.scorer.dto.game.toBaseRunningResponse
import com.nextup.scorer.dto.game.toCurrentGameStateResponse
import com.nextup.scorer.dto.game.toCurrentLineupResponse
import com.nextup.scorer.dto.game.toDomain
import com.nextup.scorer.dto.game.toEventTimelineResponse
import com.nextup.scorer.dto.game.toResponse
import com.nextup.scorer.dto.game.toScoreboardResponse
import com.nextup.scorer.dto.game.toSubstitutionResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 기록원 전용 경기 기록 컨트롤러
 *
 * 실시간 경기 기록 입력 및 경기 상태 조회를 위한 API를 제공합니다.
 * 모든 기록(POST) API는 scorerId 파라미터를 통해 기록원 독점 잠금 검증을 수행합니다.
 * GET 엔드포인트는 기록원 재접속 시 현재 상태를 복원하거나,
 * WebSocket 단절 시 REST fallback으로 활용됩니다.
 */
@RestController
@RequestMapping("/api/v1/scorer/games")
class GameScorerController(
    private val gameLifecycleService: GameLifecycleService,
    private val plateAppearanceRecordService: PlateAppearanceRecordService,
    private val gameUndoService: GameUndoService,
    private val baseRunningRecordService: BaseRunningRecordService,
    private val gameSubstitutionService: GameSubstitutionService,
    private val gameStateQueryService: GameStateQueryService,
    private val gameTimelineService: GameTimelineService,
) {
    // ===== GET 조회 엔드포인트 (H-16, M-25) =====

    /**
     * 현재 경기 상태를 조회합니다.
     *
     * 기록원 재접속 시 이닝, 아웃, 주자, 볼카운트 등 현재 경기 상태를 복원합니다.
     */
    @GetMapping("/{gameId}/state")
    @ResponseStatus(HttpStatus.OK)
    fun getGameState(
        @PathVariable gameId: Long,
    ): ApiResponse<CurrentGameStateResponse> {
        val game = gameStateQueryService.getGame(gameId)
        val gameTeams = gameStateQueryService.getGameTeams(gameId)
        return ApiResponse.success(game.toCurrentGameStateResponse(gameTeams))
    }

    /**
     * 현재 라인업을 조회합니다.
     *
     * 경기에 현재 출전 중인 선수 목록을 홈/원정으로 분리하여 반환합니다.
     */
    @GetMapping("/{gameId}/lineup")
    @ResponseStatus(HttpStatus.OK)
    fun getCurrentLineup(
        @PathVariable gameId: Long,
    ): ApiResponse<CurrentLineupResponse> {
        val players = gameStateQueryService.getCurrentLineup(gameId)
        return ApiResponse.success(toCurrentLineupResponse(gameId, players))
    }

    /**
     * 이벤트 타임라인을 조회합니다.
     *
     * 경기의 모든 이벤트를 시간 순서대로 반환합니다.
     * 이닝 범위를 지정하여 부분 조회할 수 있습니다.
     */
    @GetMapping("/{gameId}/events")
    @ResponseStatus(HttpStatus.OK)
    fun getEventTimeline(
        @PathVariable gameId: Long,
        @RequestParam(required = false) fromInning: Int?,
        @RequestParam(required = false) toInning: Int?,
    ): ApiResponse<EventTimelineResponse> {
        val timeline = gameTimelineService.getTimeline(gameId, fromInning, toInning)
        return ApiResponse.success(timeline.toEventTimelineResponse())
    }

    /**
     * 스코어보드를 조회합니다 (REST fallback).
     *
     * WebSocket 단절 시 현재 스코어보드를 REST로 조회할 수 있습니다.
     */
    @GetMapping("/{gameId}/scoreboard")
    @ResponseStatus(HttpStatus.OK)
    fun getScoreboard(
        @PathVariable gameId: Long,
    ): ApiResponse<ScoreboardResponse> {
        val game = gameStateQueryService.getGame(gameId)
        val gameTeams = gameStateQueryService.getGameTeams(gameId)
        return ApiResponse.success(game.toScoreboardResponse(gameTeams))
    }

    // ===== POST 기록 엔드포인트 =====

    /**
     * 기록원이 경기를 독점 잠금합니다.
     *
     * 다른 기록원이 이미 잠금한 경우 409 Conflict를 반환합니다.
     * 동일 기록원의 중복 잠금 시도는 멱등하게 처리됩니다.
     */
    @PostMapping("/{gameId}/lock")
    @ResponseStatus(HttpStatus.OK)
    fun lockGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
    ): ApiResponse<GameResponse> {
        val game = gameLifecycleService.lockGame(gameId, scorerId)
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 기록원의 경기 잠금을 해제합니다.
     *
     * 잠금한 기록원 본인만 해제할 수 있습니다.
     */
    @PostMapping("/{gameId}/unlock")
    @ResponseStatus(HttpStatus.OK)
    fun unlockGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
    ): ApiResponse<GameResponse> {
        val game = gameLifecycleService.unlockGame(gameId, scorerId)
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 경기를 시작합니다.
     *
     * 경기를 잠금한 기록원만 시작할 수 있습니다.
     */
    @PostMapping("/{gameId}/start")
    @ResponseStatus(HttpStatus.OK)
    fun startGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
    ): ApiResponse<GameResponse> {
        val game = gameLifecycleService.startGame(gameId, scorerId)
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 타석 결과를 입력합니다.
     *
     * 경기를 잠금한 기록원만 기록할 수 있습니다.
     * 응답에 경고 메시지(투구 수 경고, 타순 위반 경고)가 포함될 수 있습니다.
     */
    @PostMapping("/{gameId}/plate-appearances")
    @ResponseStatus(HttpStatus.OK)
    fun recordPlateAppearance(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody @Valid request: PlateAppearanceRequestDto,
    ): ApiResponse<GameResponse> {
        val result =
            plateAppearanceRecordService.recordPlateAppearance(gameId, request.toDomain(), scorerId)
        return ApiResponse.success(result.game.toResponse(warnings = result.warnings))
    }

    /**
     * 반 이닝을 진행합니다 (공수 교대).
     *
     * 경기를 잠금한 기록원만 진행할 수 있습니다.
     */
    @PostMapping("/{gameId}/half-inning")
    @ResponseStatus(HttpStatus.OK)
    fun advanceHalfInning(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
    ): ApiResponse<GameResponse> {
        val game = gameLifecycleService.advanceHalfInning(gameId, scorerId)
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 경기를 종료합니다.
     *
     * 경기를 잠금한 기록원만 종료할 수 있습니다.
     */
    @PostMapping("/{gameId}/end")
    @ResponseStatus(HttpStatus.OK)
    fun endGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody @Valid request: GameEndRequestDto,
    ): ApiResponse<GameResponse> {
        val game = gameLifecycleService.endGame(gameId, request.reason!!, scorerId)
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 마지막 이벤트를 되돌립니다 (Undo).
     *
     * 경기를 잠금한 기록원만 되돌릴 수 있습니다.
     */
    @PostMapping("/{gameId}/undo")
    @ResponseStatus(HttpStatus.OK)
    fun undoLastEvent(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
    ): ApiResponse<UndoResponse> {
        val undoneEvent = gameUndoService.undoLastEvent(gameId, scorerId)
        val game = undoneEvent.game
        return ApiResponse.success(
            UndoResponse(
                undoneEventId = undoneEvent.id,
                eventType = undoneEvent.eventType.displayName,
                restoredState = game.gameState.toResponse(),
                message = "${undoneEvent.eventType.displayName} 이벤트가 되돌려졌습니다.",
            ),
        )
    }

    /**
     * 주루 플레이를 기록합니다.
     *
     * 경기를 잠금한 기록원만 기록할 수 있습니다.
     * 도루, 도루 실패, 견제사, 폭투 진루 등 타석 외 주루 이벤트를 기록합니다.
     */
    @PostMapping("/{gameId}/base-running")
    @ResponseStatus(HttpStatus.OK)
    fun recordBaseRunning(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody @Valid request: BaseRunningRequestDto,
    ): ApiResponse<BaseRunningResponse> {
        val event = baseRunningRecordService.recordBaseRunning(gameId, request.toDomain(), scorerId)
        return ApiResponse.success(event.toBaseRunningResponse())
    }

    /**
     * 경기를 몰수 처리합니다.
     *
     * 경기를 잠금한 기록원만 몰수 처리할 수 있습니다.
     * 승리팀에 7점, 패배팀에 0점을 자동 반영하고 경기를 종료합니다.
     * 몰수 시점까지의 개인 타격/투구 기록은 공식 기록으로 유효합니다 (KBO/MLB 기준).
     */
    @PostMapping("/{gameId}/forfeit")
    @ResponseStatus(HttpStatus.OK)
    fun forfeitGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody @Valid request: ForfeitRequestDto,
    ): ApiResponse<GameResponse> {
        val game =
            gameLifecycleService.forfeitGame(
                gameId = gameId,
                winnerTeamId = request.winnerTeamId!!,
                reason = request.reason!!,
                scorerId = scorerId,
            )
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 경기를 취소합니다.
     *
     * 경기를 잠금한 기록원만 취소할 수 있습니다.
     * 예정(SCHEDULED) 또는 연기(POSTPONED) 상태의 경기만 취소 가능합니다.
     * 취소된 경기에 실시간 반영된 시즌 타격/투구 통계는 자동으로 롤백됩니다.
     */
    @PostMapping("/{gameId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    fun cancelGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody request: CancelGameRequestDto = CancelGameRequestDto(),
    ): ApiResponse<GameResponse> {
        val game =
            gameLifecycleService.cancelGame(
                gameId = gameId,
                reason = request.reason,
                scorerId = scorerId,
            )
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 경기를 중단합니다.
     *
     * 경기를 잠금한 기록원만 중단할 수 있습니다.
     * 진행 중(IN_PROGRESS) 상태의 경기를 우천 등의 사유로 중단합니다.
     * 중단된 경기는 resume API로 재개할 수 있습니다.
     */
    @PostMapping("/{gameId}/suspend")
    @ResponseStatus(HttpStatus.OK)
    fun suspendGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody request: SuspendGameRequestDto = SuspendGameRequestDto(),
    ): ApiResponse<GameResponse> {
        val game =
            gameLifecycleService.suspendGame(
                gameId = gameId,
                reason = request.reason,
                scorerId = scorerId,
            )
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 중단된 경기를 재개합니다.
     *
     * 경기를 잠금한 기록원만 재개할 수 있습니다.
     * SUSPENDED 상태의 경기를 중단 시점의 이닝/아웃/주자 상태부터 이어서 진행합니다.
     */
    @PostMapping("/{gameId}/resume")
    @ResponseStatus(HttpStatus.OK)
    fun resumeGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
    ): ApiResponse<GameResponse> {
        val game = gameLifecycleService.resumeGame(gameId = gameId, scorerId = scorerId)
        return ApiResponse.success(game.toResponse())
    }

    /**
     * 선수를 교체합니다.
     *
     * 경기를 잠금한 기록원만 교체할 수 있습니다.
     * 교체 이벤트를 기록하고 다음을 검증합니다:
     * - 퇴장한 선수의 재출전 방지
     * - DH 해제 규칙 (투수가 DH 타순으로만 교체 가능)
     */
    @PostMapping("/{gameId}/substitutions")
    @ResponseStatus(HttpStatus.OK)
    fun substitutePlayer(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody @Valid request: SubstitutionRequestDto,
    ): ApiResponse<SubstitutionResponse> {
        val event =
            gameSubstitutionService.substitutePlayer(gameId, request.toDomain(), scorerId)
        return ApiResponse.success(event.toSubstitutionResponse())
    }
}
