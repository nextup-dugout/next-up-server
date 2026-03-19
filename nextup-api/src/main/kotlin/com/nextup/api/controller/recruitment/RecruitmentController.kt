package com.nextup.api.controller.recruitment

import com.nextup.api.dto.recruitment.CreateRecruitmentApiRequest
import com.nextup.api.dto.recruitment.RecruitmentResponse
import com.nextup.api.dto.recruitment.UpdateRecruitmentApiRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.recruitment.TeamRecruitmentService
import com.nextup.core.service.recruitment.dto.CreateRecruitmentRequest
import com.nextup.core.service.recruitment.dto.UpdateRecruitmentRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 팀 모집 공고 API Controller (공개 API)
 *
 * 팀의 선수 모집 공고 관리
 */
@RestController
@RequestMapping("/api/v1")
class RecruitmentController(
    private val recruitmentService: TeamRecruitmentService,
) {
    /**
     * 모든 진행 중인 모집 공고를 조회합니다.
     */
    @GetMapping("/recruitments")
    fun getAllOpenRecruitments(): ApiResponse<List<RecruitmentResponse>> {
        val recruitments = recruitmentService.getAllOpen()
        return ApiResponse.success(recruitments.map { RecruitmentResponse.from(it) })
    }

    /**
     * 모집 공고 상세를 조회합니다.
     */
    @GetMapping("/recruitments/{id}")
    fun getRecruitment(
        @PathVariable id: Long,
    ): ApiResponse<RecruitmentResponse> {
        val recruitment = recruitmentService.getById(id)
        return ApiResponse.success(RecruitmentResponse.from(recruitment))
    }

    /**
     * 팀의 모집 공고를 생성합니다.
     */
    @PostMapping("/teams/{teamId}/recruitments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#teamId, authentication.principal)")
    fun createRecruitment(
        @PathVariable teamId: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateRecruitmentApiRequest,
    ): ApiResponse<RecruitmentResponse> {
        val recruitment =
            recruitmentService.createRecruitment(
                CreateRecruitmentRequest(
                    teamId = teamId,
                    title = request.title,
                    description = request.description,
                    positionsNeeded = request.positionsNeeded,
                    ageRange = request.ageRange,
                    skillLevel = request.skillLevel,
                    location = request.location,
                    deadline = request.deadline,
                ),
            )

        return ApiResponse.success(RecruitmentResponse.from(recruitment))
    }

    /**
     * 팀의 모집 공고를 수정합니다.
     */
    @PutMapping("/teams/{teamId}/recruitments/{id}")
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#teamId, authentication.principal)")
    fun updateRecruitment(
        @PathVariable teamId: Long,
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: UpdateRecruitmentApiRequest,
    ): ApiResponse<RecruitmentResponse> {
        val recruitment =
            recruitmentService.updateRecruitment(
                id = id,
                request =
                    UpdateRecruitmentRequest(
                        title = request.title,
                        description = request.description,
                        positionsNeeded = request.positionsNeeded,
                        deadline = request.deadline,
                    ),
            )

        return ApiResponse.success(RecruitmentResponse.from(recruitment))
    }

    /**
     * 팀의 모집 공고를 삭제합니다.
     */
    @DeleteMapping("/teams/{teamId}/recruitments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#teamId, authentication.principal)")
    fun deleteRecruitment(
        @PathVariable teamId: Long,
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<Unit> {
        recruitmentService.deleteRecruitment(id)
        return ApiResponse.success(Unit)
    }
}
