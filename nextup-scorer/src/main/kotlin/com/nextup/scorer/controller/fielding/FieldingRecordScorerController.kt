package com.nextup.scorer.controller.fielding

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.FieldingRecordService
import com.nextup.scorer.dto.fielding.FieldingEventRequest
import com.nextup.scorer.dto.fielding.FieldingEventType
import com.nextup.scorer.dto.fielding.FieldingRecordResponse
import com.nextup.scorer.dto.fielding.toScorerResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 기록원 전용 수비 기록 컨트롤러
 *
 * 실시간 경기에서 수비 이벤트(자살/보살/실책/병살/포일)를 기록합니다.
 */
@RestController
@RequestMapping("/api/v1/scorer/games/{gameId}/fielding")
@PreAuthorize("isAuthenticated()")
class FieldingRecordScorerController(
    private val fieldingRecordService: FieldingRecordService,
) {
    /**
     * 수비 기록을 생성합니다 (경기 시작 시 선수별 초기화).
     *
     * POST /api/v1/scorer/games/{gameId}/fielding/players/{playerId}
     */
    @PostMapping("/players/{playerId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFieldingRecord(
        @PathVariable gameId: Long,
        @PathVariable playerId: Long,
        @AuthenticationPrincipal scorerId: Long,
    ): ApiResponse<FieldingRecordResponse> {
        val record = fieldingRecordService.createRecordByGameAndPlayer(gameId, playerId)
        return ApiResponse.success(record.toScorerResponse())
    }

    /**
     * 수비 이벤트를 기록합니다.
     *
     * POST /api/v1/scorer/games/{gameId}/fielding/events
     */
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.OK)
    fun recordFieldingEvent(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @Valid @RequestBody request: FieldingEventRequest,
    ): ApiResponse<FieldingRecordResponse> {
        val gamePlayerId = request.gamePlayerId!!
        when (request.eventType!!) {
            FieldingEventType.PUT_OUT -> fieldingRecordService.recordPutOut(gamePlayerId)
            FieldingEventType.ASSIST -> fieldingRecordService.recordAssist(gamePlayerId)
            FieldingEventType.ERROR -> fieldingRecordService.recordError(gamePlayerId)
            FieldingEventType.DOUBLE_PLAY -> fieldingRecordService.recordDoublePlay(gamePlayerId)
            FieldingEventType.PASSED_BALL -> fieldingRecordService.recordPassedBall(gamePlayerId)
        }
        val record = fieldingRecordService.getByGamePlayerId(gamePlayerId)
        return ApiResponse.success(record.toScorerResponse())
    }

    /**
     * 경기의 모든 수비 기록을 조회합니다.
     *
     * GET /api/v1/scorer/games/{gameId}/fielding
     */
    @GetMapping
    fun getFieldingRecordsByGame(
        @PathVariable gameId: Long,
    ): ApiResponse<List<FieldingRecordResponse>> {
        val records = fieldingRecordService.getAllByGameId(gameId)
        return ApiResponse.success(records.toScorerResponse())
    }
}
