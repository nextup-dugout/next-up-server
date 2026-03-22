package com.nextup.scorer.controller.correction

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.correction.CorrectionRequestService
import com.nextup.scorer.dto.correction.CorrectionRequestResponse
import com.nextup.scorer.dto.correction.CreateCorrectionRequestDto
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
 * 기록 정정 요청 Controller (기록원 전용)
 *
 * 기록원이 기록 정정을 요청하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/scorer/corrections/requests")
class CorrectionRequestScorerController(
    private val correctionRequestService: CorrectionRequestService,
) {
    /**
     * 기록 정정 요청을 생성합니다.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCorrectionRequest(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateCorrectionRequestDto,
    ): ApiResponse<CorrectionRequestResponse> {
        val correctionRequest =
            correctionRequestService.createRequest(
                gameId = request.gameId,
                requesterUserId = userId,
                correctionType = request.correctionType,
                targetRecordId = request.targetRecordId,
                fieldName = request.fieldName,
                newValue = request.newValue,
                reason = request.reason,
            )
        return ApiResponse.success(CorrectionRequestResponse.from(correctionRequest))
    }

    /**
     * 경기별 정정 요청 목록을 조회합니다.
     */
    @GetMapping("/games/{gameId}")
    fun getCorrectionRequestsByGame(
        @PathVariable gameId: Long,
    ): ApiResponse<List<CorrectionRequestResponse>> {
        val requests = correctionRequestService.getByGameId(gameId)
        return ApiResponse.success(requests.map { CorrectionRequestResponse.from(it) })
    }
}
