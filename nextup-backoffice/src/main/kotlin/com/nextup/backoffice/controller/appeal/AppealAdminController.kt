package com.nextup.backoffice.controller.appeal

import com.nextup.backoffice.dto.appeal.AppealAdminResponse
import com.nextup.backoffice.dto.appeal.ApproveAppealAdminRequest
import com.nextup.backoffice.dto.appeal.RejectAppealAdminRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.domain.appeal.AppealStatus
import com.nextup.core.service.appeal.AppealService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 이의 제기 관리 API Controller (관리자용)
 *
 * 전체 권한: 조회, 승인, 반려
 */
@RestController
@RequestMapping("/api/backoffice/appeals")
class AppealAdminController(
    private val appealService: AppealService,
) {
    /**
     * 전체 이의 제기 목록을 조회합니다.
     */
    @GetMapping
    fun getAllAppeals(
        @RequestParam(required = false) status: AppealStatus?,
    ): ApiResponse<List<AppealAdminResponse>> {
        val appeals =
            if (status != null) {
                appealService.getAppealsByStatus(status)
            } else {
                appealService.getAllAppeals()
            }

        return ApiResponse.success(
            appeals.map { AppealAdminResponse.from(it) },
        )
    }

    /**
     * 이의 제기 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getAppeal(
        @PathVariable id: Long,
    ): ApiResponse<AppealAdminResponse> {
        val appeal = appealService.getById(id)
        return ApiResponse.success(AppealAdminResponse.from(appeal))
    }

    /**
     * 이의 제기를 승인합니다.
     */
    @PutMapping("/{id}/approve")
    fun approveAppeal(
        @PathVariable id: Long,
        @Valid @RequestBody request: ApproveAppealAdminRequest,
    ): ApiResponse<AppealAdminResponse> {
        val appeal =
            appealService.approveAppeal(
                appealId = id,
                reviewerId = request.reviewerId,
                comment = request.comment,
            )

        return ApiResponse.success(AppealAdminResponse.from(appeal))
    }

    /**
     * 이의 제기를 반려합니다.
     */
    @PutMapping("/{id}/reject")
    fun rejectAppeal(
        @PathVariable id: Long,
        @Valid @RequestBody request: RejectAppealAdminRequest,
    ): ApiResponse<AppealAdminResponse> {
        val appeal =
            appealService.rejectAppeal(
                appealId = id,
                reviewerId = request.reviewerId,
                comment = request.comment,
            )

        return ApiResponse.success(AppealAdminResponse.from(appeal))
    }
}
