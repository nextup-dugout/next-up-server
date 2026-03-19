package com.nextup.api.controller.mercenary

import com.nextup.api.dto.mercenary.ApplyMercenaryApiRequest
import com.nextup.api.dto.mercenary.CreateMercenaryRequestApiRequest
import com.nextup.api.dto.mercenary.MercenaryApplicationResponse
import com.nextup.api.dto.mercenary.MercenaryParticipationResponse
import com.nextup.api.dto.mercenary.MercenaryRequestResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.mercenary.MercenaryService
import com.nextup.core.service.mercenary.dto.ApplyMercenaryDto
import com.nextup.core.service.mercenary.dto.CreateMercenaryRequestDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 용병 요청 API Controller (공개 API)
 *
 * 팀의 용병 모집 및 지원을 위한 API
 */
@RestController
@RequestMapping("/api/v1/mercenary-requests")
class MercenaryRequestController(
    private val mercenaryService: MercenaryService,
) {
    /**
     * 용병 요청을 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#request.teamId, authentication.principal)")
    fun createRequest(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateMercenaryRequestApiRequest,
    ): ApiResponse<MercenaryRequestResponse> {
        val mercenaryRequest =
            mercenaryService.createRequest(
                CreateMercenaryRequestDto(
                    requestingTeamId = request.teamId,
                    gameId = request.gameId,
                    positions = request.positions,
                    maxCount = request.maxCount,
                    deadline = request.deadline,
                    description = request.description,
                ),
            )

        return ApiResponse.success(MercenaryRequestResponse.from(mercenaryRequest))
    }

    /**
     * OPEN 상태의 용병 요청 목록을 조회합니다.
     */
    @GetMapping
    fun getOpenRequests(): ApiResponse<List<MercenaryRequestResponse>> {
        val requests = mercenaryService.getOpenRequests()
        return ApiResponse.success(requests.map { MercenaryRequestResponse.from(it) })
    }

    /**
     * 용병 요청에 지원합니다.
     */
    @PostMapping("/{id}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    fun apply(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ApplyMercenaryApiRequest,
    ): ApiResponse<MercenaryApplicationResponse> {
        val application =
            mercenaryService.apply(
                ApplyMercenaryDto(
                    requestId = id,
                    playerId = request.playerId,
                    preferredPositions = request.preferredPositions,
                    message = request.message,
                ),
            )

        return ApiResponse.success(MercenaryApplicationResponse.from(application))
    }

    /**
     * 용병 지원을 수락합니다.
     */
    @PatchMapping("/{id}/applications/{appId}/accept")
    @PreAuthorize("isAuthenticated()")
    fun acceptApplication(
        @PathVariable id: Long,
        @PathVariable appId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<MercenaryApplicationResponse> {
        val application = mercenaryService.acceptApplication(id, appId)
        return ApiResponse.success(MercenaryApplicationResponse.from(application))
    }

    /**
     * 용병 지원을 거절합니다.
     */
    @PatchMapping("/{id}/applications/{appId}/reject")
    @PreAuthorize("isAuthenticated()")
    fun rejectApplication(
        @PathVariable id: Long,
        @PathVariable appId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<MercenaryApplicationResponse> {
        val application = mercenaryService.rejectApplication(id, appId)
        return ApiResponse.success(MercenaryApplicationResponse.from(application))
    }

    /**
     * 용병 요청에 대한 지원 목록을 조회합니다.
     */
    @GetMapping("/{id}/applications")
    fun getApplications(
        @PathVariable id: Long,
    ): ApiResponse<List<MercenaryApplicationResponse>> {
        val applications = mercenaryService.getApplicationsByRequest(id)
        return ApiResponse.success(applications.map { MercenaryApplicationResponse.from(it) })
    }

    /**
     * 내 용병 참가 이력을 조회합니다.
     */
    @GetMapping("/me/history")
    fun getMyHistory(
        @RequestParam playerId: Long,
    ): ApiResponse<List<MercenaryParticipationResponse>> {
        val participations = mercenaryService.getParticipationsByPlayer(playerId)
        return ApiResponse.success(participations.map { MercenaryParticipationResponse.from(it) })
    }
}
