package com.nextup.scorer.controller

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.PitchEventService
import com.nextup.scorer.dto.pitch.PitchEventResponse
import com.nextup.scorer.dto.pitch.RecordPitchRequest
import com.nextup.scorer.dto.pitch.toResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * 투구 이벤트 기록 Controller (Scorer)
 *
 * 기록원이 투구를 기록하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/scorer/games/{gameId}/pitch-events")
@PreAuthorize("isAuthenticated()")
class PitchEventRecordController(
    private val pitchEventService: PitchEventService,
) {
    /**
     * 투구를 기록합니다.
     */
    @PostMapping
    fun recordPitch(
        @PathVariable gameId: Long,
        @RequestBody @Valid request: RecordPitchRequest,
    ): ResponseEntity<ApiResponse<PitchEventResponse>> {
        val pitchEvent =
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = request.pitcherId,
                batterId = request.batterId,
                result = request.result,
                description = request.description,
            )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(pitchEvent.toResponse()))
    }
}
