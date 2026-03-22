package com.nextup.api.controller.appeal

import com.nextup.api.dto.appeal.AppealResponse
import com.nextup.api.dto.appeal.CreateAppealApiRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.appeal.AppealService
import com.nextup.core.service.appeal.dto.CreateAppealRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 이의 제기 API Controller (공개 API)
 *
 * 선수/감독의 이의 제기 신청 및 조회
 */
@RestController
@RequestMapping("/api/v1")
class AppealController(
    private val appealService: AppealService,
) {
    /**
     * 경기에 대한 이의 제기를 신청합니다.
     */
    @PostMapping("/games/{gameId}/appeals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    fun createAppeal(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateAppealApiRequest,
    ): ApiResponse<AppealResponse> {
        val appeal =
            appealService.createAppeal(
                CreateAppealRequest(
                    gameId = gameId,
                    appealerId = userId,
                    appealerName = request.appealerName,
                    type = request.type,
                    title = request.title,
                    description = request.description,
                ),
            )

        return ApiResponse.success(AppealResponse.from(appeal))
    }

    /**
     * 내 이의 제기 목록을 조회합니다.
     */
    @GetMapping("/appeals")
    fun getMyAppeals(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<AppealResponse>> {
        val appeals = appealService.getAppealsByAppealer(userId)
        return ApiResponse.success(appeals.map { AppealResponse.from(it) })
    }

    /**
     * 경기별 이의 제기 목록을 조회합니다.
     */
    @GetMapping("/games/{gameId}/appeals")
    fun getGameAppeals(
        @PathVariable gameId: Long,
    ): ApiResponse<List<AppealResponse>> {
        val appeals = appealService.getAppealsByGame(gameId)
        return ApiResponse.success(appeals.map { AppealResponse.from(it) })
    }
}
