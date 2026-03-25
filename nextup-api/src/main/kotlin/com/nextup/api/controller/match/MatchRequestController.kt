package com.nextup.api.controller.match

import com.nextup.api.dto.match.CreateMatchRequestApiRequest
import com.nextup.api.dto.match.MatchRequestDetailResponse
import com.nextup.api.dto.match.MatchRequestResponse
import com.nextup.api.dto.match.MatchResponseResponse
import com.nextup.api.dto.match.RespondToMatchRequestApiRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.domain.match.SkillLevel
import com.nextup.core.service.match.MatchingService
import com.nextup.core.service.match.dto.CreateMatchRequestDto
import com.nextup.core.service.match.dto.CreateMatchResponseDto
import com.nextup.core.service.match.dto.MatchRequestFilterDto
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 매칭 요청 API Controller (공개 API)
 *
 * 팀 간 연습 경기 매칭을 위한 API
 */
@RestController
@RequestMapping("/api/v1/match-requests")
class MatchRequestController(
    private val matchingService: MatchingService,
) {
    /**
     * 매칭 요청을 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#request.teamId, authentication.principal)")
    fun createMatchRequest(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateMatchRequestApiRequest,
    ): ApiResponse<MatchRequestResponse> {
        val matchRequest =
            matchingService.createRequest(
                CreateMatchRequestDto(
                    teamId = request.teamId,
                    preferredDate = request.preferredDate,
                    preferredTime = request.preferredTime,
                    preferredLocation = request.preferredLocation,
                    message = request.message,
                    skillLevel = request.skillLevel,
                ),
            )

        return ApiResponse.success(MatchRequestResponse.from(matchRequest))
    }

    /**
     * OPEN 상태의 매칭 요청을 조회합니다.
     *
     * @param area 지역 필터 (선호 장소에 포함된 문자열)
     * @param date 날짜 필터 (선호 날짜)
     * @param skillLevel 실력 수준 필터
     */
    @GetMapping
    fun getOpenMatchRequests(
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
        @RequestParam(required = false) skillLevel: SkillLevel?,
    ): ApiResponse<List<MatchRequestResponse>> {
        val filter =
            MatchRequestFilterDto(
                area = area,
                date = date,
                skillLevel = skillLevel,
            )
        val matchRequests = matchingService.getOpenRequests(filter)
        return ApiResponse.success(matchRequests.map { MatchRequestResponse.from(it) })
    }

    /**
     * 매칭 요청 상세 정보를 조회합니다 (응답 목록 포함).
     */
    @GetMapping("/{id}")
    fun getMatchRequestDetail(
        @PathVariable id: Long,
    ): ApiResponse<MatchRequestDetailResponse> {
        val matchRequest = matchingService.getRequestById(id)
        val responses = matchingService.getResponsesByRequest(id)

        return ApiResponse.success(MatchRequestDetailResponse.from(matchRequest, responses))
    }

    /**
     * 매칭 요청에 응답합니다.
     */
    @PostMapping("/{id}/respond")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@teamSecurity.isMember(#request.respondTeamId, authentication.principal)")
    fun respondToMatchRequest(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: RespondToMatchRequestApiRequest,
    ): ApiResponse<MatchResponseResponse> {
        val matchResponse =
            matchingService.respondToRequest(
                CreateMatchResponseDto(
                    matchRequestId = id,
                    respondTeamId = request.respondTeamId,
                    message = request.message,
                ),
            )

        return ApiResponse.success(MatchResponseResponse.from(matchResponse))
    }

    /**
     * 매칭 응답을 수락합니다.
     * 매칭 요청 팀의 OWNER/MANAGER만 수락할 수 있습니다 (소유권 검증).
     */
    @PutMapping("/{id}/accept/{responseId}")
    @PreAuthorize("isAuthenticated()")
    fun acceptMatchResponse(
        @PathVariable id: Long,
        @PathVariable responseId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<MatchRequestResponse> {
        val matchRequest = matchingService.acceptResponse(id, responseId, userId)
        return ApiResponse.success(MatchRequestResponse.from(matchRequest))
    }

    /**
     * 매칭 요청을 취소합니다.
     * 매칭 요청 팀의 OWNER/MANAGER만 취소할 수 있습니다 (소유권 검증).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun cancelMatchRequest(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<MatchRequestResponse> {
        val matchRequest = matchingService.cancelRequest(id, userId)
        return ApiResponse.success(MatchRequestResponse.from(matchRequest))
    }
}
