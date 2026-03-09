package com.nextup.api.controller

import com.nextup.api.dto.election.CandidateResponse
import com.nextup.api.dto.election.CastVoteApiRequest
import com.nextup.api.dto.election.CreateElectionApiRequest
import com.nextup.api.dto.election.ElectionResponse
import com.nextup.api.dto.election.ElectionResultResponse
import com.nextup.api.dto.election.RegisterCandidateApiRequest
import com.nextup.api.dto.election.toElectionResponseList
import com.nextup.api.dto.election.toResponse
import com.nextup.api.dto.election.toServiceRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.election.ElectionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Election API Controller
 *
 * 팀 내 선거/투표 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/teams/{teamId}/elections")
class ElectionController(
    private val electionService: ElectionService,
) {
    /**
     * 선거를 생성합니다.
     */
    @PostMapping
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#teamId, authentication.principal)")
    fun createElection(
        @PathVariable teamId: Long,
        @RequestBody @Valid request: CreateElectionApiRequest,
    ): ResponseEntity<ApiResponse<ElectionResponse>> {
        val election = electionService.createElection(request.toServiceRequest(teamId))
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(election.toResponse()))
    }

    /**
     * 팀의 모든 선거를 조회합니다.
     */
    @GetMapping
    @PreAuthorize("@teamSecurity.isMember(#teamId, authentication.principal)")
    fun getElectionsByTeam(
        @PathVariable teamId: Long,
    ): ApiResponse<List<ElectionResponse>> =
        ApiResponse.success(electionService.getElectionsByTeam(teamId).toElectionResponseList())

    /**
     * 선거를 ID로 조회합니다.
     */
    @GetMapping("/{electionId}")
    @PreAuthorize("@teamSecurity.isMember(#teamId, authentication.principal)")
    fun getElection(
        @PathVariable teamId: Long,
        @PathVariable electionId: Long,
    ): ApiResponse<ElectionResponse> = ApiResponse.success(electionService.getElectionById(electionId).toResponse())

    /**
     * 선거를 시작합니다.
     */
    @PutMapping("/{electionId}/start")
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#teamId, authentication.principal)")
    fun startElection(
        @PathVariable teamId: Long,
        @PathVariable electionId: Long,
    ): ApiResponse<ElectionResponse> = ApiResponse.success(electionService.startElection(electionId).toResponse())

    /**
     * 선거를 완료합니다.
     */
    @PutMapping("/{electionId}/complete")
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#teamId, authentication.principal)")
    fun completeElection(
        @PathVariable teamId: Long,
        @PathVariable electionId: Long,
    ): ApiResponse<ElectionResponse> = ApiResponse.success(electionService.completeElection(electionId).toResponse())

    /**
     * 선거를 취소합니다.
     */
    @PutMapping("/{electionId}/cancel")
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#teamId, authentication.principal)")
    fun cancelElection(
        @PathVariable teamId: Long,
        @PathVariable electionId: Long,
    ): ApiResponse<ElectionResponse> = ApiResponse.success(electionService.cancelElection(electionId).toResponse())

    /**
     * 후보자를 등록합니다.
     */
    @PostMapping("/{electionId}/candidates")
    @PreAuthorize("@teamSecurity.isMember(#teamId, authentication.principal)")
    fun registerCandidate(
        @PathVariable teamId: Long,
        @PathVariable electionId: Long,
        @RequestBody @Valid request: RegisterCandidateApiRequest,
    ): ResponseEntity<ApiResponse<CandidateResponse>> {
        val candidate = electionService.registerCandidate(request.toServiceRequest(electionId))
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(candidate.toResponse()))
    }

    /**
     * 투표합니다.
     */
    @PostMapping("/{electionId}/votes")
    @PreAuthorize("@teamSecurity.isMember(#teamId, authentication.principal)")
    fun castVote(
        @PathVariable teamId: Long,
        @PathVariable electionId: Long,
        @RequestBody @Valid request: CastVoteApiRequest,
    ): ResponseEntity<ApiResponse<CandidateResponse>> {
        val candidate = electionService.vote(request.toServiceRequest(electionId))
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(candidate.toResponse()))
    }

    /**
     * 선거 결과를 조회합니다.
     */
    @GetMapping("/{electionId}/results")
    @PreAuthorize("@teamSecurity.isMember(#teamId, authentication.principal)")
    fun getResults(
        @PathVariable teamId: Long,
        @PathVariable electionId: Long,
    ): ApiResponse<ElectionResultResponse> = ApiResponse.success(electionService.getResults(electionId).toResponse())
}
