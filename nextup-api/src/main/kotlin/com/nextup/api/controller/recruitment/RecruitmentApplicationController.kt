package com.nextup.api.controller.recruitment

import com.nextup.api.dto.recruitment.ApplyRecruitmentApiRequest
import com.nextup.api.dto.recruitment.RecruitmentApplicationResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.recruitment.RecruitmentApplicationService
import com.nextup.core.service.recruitment.dto.ApplyRecruitmentRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 모집 공고 지원 API Controller (공개 API)
 *
 * 모집 공고 지원 관련 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1")
class RecruitmentApplicationController(
    private val applicationService: RecruitmentApplicationService,
) {
    /**
     * 모집 공고에 지원합니다.
     */
    @PostMapping("/recruitments/{recruitmentId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    fun apply(
        @PathVariable recruitmentId: Long,
        @RequestParam applicantId: Long,
        @Valid @RequestBody request: ApplyRecruitmentApiRequest,
    ): ApiResponse<RecruitmentApplicationResponse> {
        val application =
            applicationService.apply(
                ApplyRecruitmentRequest(
                    recruitmentId = recruitmentId,
                    applicantId = applicantId,
                    message = request.message,
                    preferredPositions = request.preferredPositions,
                ),
            )
        return ApiResponse.success(RecruitmentApplicationResponse.from(application))
    }

    /**
     * 내 지원 현황을 조회합니다.
     */
    @GetMapping("/me/applications")
    fun getMyApplications(
        @RequestParam applicantId: Long,
    ): ApiResponse<List<RecruitmentApplicationResponse>> {
        val applications = applicationService.getApplicationsByApplicant(applicantId)
        return ApiResponse.success(applications.map { RecruitmentApplicationResponse.from(it) })
    }

    /**
     * 지원을 취소합니다. (지원자 본인)
     */
    @DeleteMapping("/me/applications/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdrawApplication(
        @PathVariable applicationId: Long,
        @RequestParam applicantId: Long,
    ): ApiResponse<Unit> {
        applicationService.withdrawApplication(applicationId, applicantId)
        return ApiResponse.success(Unit)
    }

    /**
     * 모집 공고별 지원자 목록을 조회합니다. (팀 관리자)
     */
    @GetMapping("/recruitments/{recruitmentId}/applications")
    fun getApplications(
        @PathVariable recruitmentId: Long,
    ): ApiResponse<List<RecruitmentApplicationResponse>> {
        val applications = applicationService.getApplicationsByRecruitment(recruitmentId)
        return ApiResponse.success(applications.map { RecruitmentApplicationResponse.from(it) })
    }

    /**
     * 지원을 수락합니다. (팀 관리자)
     */
    @PatchMapping("/recruitments/{recruitmentId}/applications/{applicationId}/accept")
    fun acceptApplication(
        @PathVariable recruitmentId: Long,
        @PathVariable applicationId: Long,
        @RequestParam processorId: Long,
    ): ApiResponse<RecruitmentApplicationResponse> {
        val application = applicationService.acceptApplication(applicationId, processorId)
        return ApiResponse.success(RecruitmentApplicationResponse.from(application))
    }

    /**
     * 지원을 거절합니다. (팀 관리자)
     */
    @PatchMapping("/recruitments/{recruitmentId}/applications/{applicationId}/reject")
    fun rejectApplication(
        @PathVariable recruitmentId: Long,
        @PathVariable applicationId: Long,
        @RequestParam processorId: Long,
    ): ApiResponse<RecruitmentApplicationResponse> {
        val application = applicationService.rejectApplication(applicationId, processorId)
        return ApiResponse.success(RecruitmentApplicationResponse.from(application))
    }
}
