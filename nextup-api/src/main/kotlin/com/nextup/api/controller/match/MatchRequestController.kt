package com.nextup.api.controller.match

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.match.CreateMatchRequestApiRequest
import com.nextup.api.dto.match.MatchRequestDetailResponse
import com.nextup.api.dto.match.MatchRequestResponse
import com.nextup.api.dto.match.MatchResponseResponse
import com.nextup.api.dto.match.RespondToMatchRequestApiRequest
import com.nextup.core.service.match.MatchingService
import com.nextup.core.service.match.dto.CreateMatchRequestDto
import com.nextup.core.service.match.dto.CreateMatchResponseDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

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
    fun createMatchRequest(
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
     * OPEN 상태의 모든 매칭 요청을 조회합니다.
     */
    @GetMapping
    fun getOpenMatchRequests(): ApiResponse<List<MatchRequestResponse>> {
        val matchRequests = matchingService.getOpenRequests()
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
    fun respondToMatchRequest(
        @PathVariable id: Long,
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
     */
    @PutMapping("/{id}/accept/{responseId}")
    fun acceptMatchResponse(
        @PathVariable id: Long,
        @PathVariable responseId: Long,
    ): ApiResponse<MatchRequestResponse> {
        val matchRequest = matchingService.acceptResponse(id, responseId)
        return ApiResponse.success(MatchRequestResponse.from(matchRequest))
    }

    /**
     * 매칭 요청을 취소합니다.
     */
    @DeleteMapping("/{id}")
    fun cancelMatchRequest(
        @PathVariable id: Long,
    ): ApiResponse<MatchRequestResponse> {
        val matchRequest = matchingService.cancelRequest(id)
        return ApiResponse.success(MatchRequestResponse.from(matchRequest))
    }
}
