package com.nextup.backoffice.controller.correction

import com.nextup.backoffice.dto.correction.ApproveCorrectionRequestDto
import com.nextup.backoffice.dto.correction.CorrectionRequestAdminResponse
import com.nextup.backoffice.dto.correction.RejectCorrectionRequestDto
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.correction.CorrectionRequestService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 기록 정정 요청 관리자 Controller (Backoffice)
 *
 * 관리자가 기록원의 정정 요청을 승인/반려하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/backoffice/corrections")
@PreAuthorize("hasRole('ADMIN')")
class CorrectionRequestAdminController(
    private val correctionRequestService: CorrectionRequestService,
) {
    /**
     * 대기 중인 정정 요청 목록을 조회합니다.
     */
    @GetMapping("/pending")
    fun getPendingRequests(): ApiResponse<List<CorrectionRequestAdminResponse>> {
        val requests = correctionRequestService.getPendingRequests()
        return ApiResponse.success(requests.map { CorrectionRequestAdminResponse.from(it) })
    }

    /**
     * 정정 요청 상세를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getCorrectionRequest(
        @PathVariable id: Long,
    ): ApiResponse<CorrectionRequestAdminResponse> {
        val request = correctionRequestService.getById(id)
        return ApiResponse.success(CorrectionRequestAdminResponse.from(request))
    }

    /**
     * 정정 요청을 승인합니다.
     *
     * 승인 시 기존 RecordCorrectionService를 통해 실제 기록 정정이 수행됩니다.
     */
    @PatchMapping("/{id}/approve")
    fun approveRequest(
        @PathVariable id: Long,
        @AuthenticationPrincipal adminUserId: Long,
        @Valid @RequestBody request: ApproveCorrectionRequestDto,
    ): ApiResponse<CorrectionRequestAdminResponse> {
        val correctionRequest =
            correctionRequestService.approve(
                requestId = id,
                reviewerUserId = adminUserId,
                comment = request.comment,
            )
        return ApiResponse.success(CorrectionRequestAdminResponse.from(correctionRequest))
    }

    /**
     * 정정 요청을 반려합니다.
     */
    @PatchMapping("/{id}/reject")
    fun rejectRequest(
        @PathVariable id: Long,
        @AuthenticationPrincipal adminUserId: Long,
        @Valid @RequestBody request: RejectCorrectionRequestDto,
    ): ApiResponse<CorrectionRequestAdminResponse> {
        val correctionRequest =
            correctionRequestService.reject(
                requestId = id,
                reviewerUserId = adminUserId,
                comment = request.comment,
            )
        return ApiResponse.success(CorrectionRequestAdminResponse.from(correctionRequest))
    }
}
