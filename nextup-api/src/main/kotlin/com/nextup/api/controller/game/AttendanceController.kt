package com.nextup.api.controller.game

import com.nextup.api.dto.attendance.*
import com.nextup.api.mapper.attendance.toGameVoteResponse
import com.nextup.api.mapper.attendance.toSummaryResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.attendance.AttendanceService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 경기 출석 투표 API 컨트롤러
 *
 * 경기별 출석 투표를 관리합니다. AttendancePoll 통합 모델을 사용합니다.
 */
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/v1/games/{gameId}/attendance")
class AttendanceController(
    private val attendanceService: AttendanceService,
) {
    /**
     * 출석 투표를 합니다.
     */
    @PostMapping
    fun vote(
        @PathVariable gameId: Long,
        @RequestBody @Valid request: AttendanceVoteRequest,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<AttendanceVoteResponse> {
        val vote =
            attendanceService.voteForGame(
                gameId = gameId,
                userId = userId,
                voteType = request.voteType,
                absenceReason = request.absenceReason,
                reasonDetail = request.reasonDetail,
            )

        return ApiResponse.success(vote.toGameVoteResponse(gameId))
    }

    /**
     * 출석 투표를 변경합니다.
     */
    @PatchMapping
    fun changeVote(
        @PathVariable gameId: Long,
        @RequestBody @Valid request: AttendanceVoteRequest,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<AttendanceVoteResponse> {
        // 투표 변경은 vote 메서드와 동일하게 처리 (기존 투표가 있으면 변경됨)
        return vote(gameId, request, userId)
    }

    /**
     * 경기의 출석 투표 현황을 조회합니다.
     * 해당 경기에 참가하는 팀의 멤버만 조회 가능합니다.
     */
    @GetMapping
    fun getVotes(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<AttendanceVotesResponse> {
        val votes = attendanceService.getGameVotes(gameId, userId)
        val summary = attendanceService.getGameVoteSummary(gameId, userId)

        val response =
            AttendanceVotesResponse(
                gameId = gameId,
                pollId = summary.pollId,
                votes = votes.toGameVoteResponse(gameId),
                summary = summary.toSummaryResponse(),
            )

        return ApiResponse.success(response)
    }

    /**
     * 경기의 출석 투표 요약을 조회합니다.
     * 해당 경기에 참가하는 팀의 멤버만 조회 가능합니다.
     */
    @GetMapping("/summary")
    fun getSummary(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<AttendanceSummaryResponse> {
        val summary = attendanceService.getGameVoteSummary(gameId, userId)
        return ApiResponse.success(summary.toSummaryResponse())
    }

    /**
     * 미투표자 목록을 조회합니다.
     * 해당 경기에 참가하는 팀의 멤버만 조회 가능합니다.
     */
    @GetMapping("/non-voters")
    fun getNonVoters(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<NonVoterResponse>> {
        val nonVoters = attendanceService.getGameNonVoters(gameId, userId)
        val response =
            nonVoters.map {
                NonVoterResponse(
                    playerId = it.player.id,
                    playerName = it.player.name,
                )
            }
        return ApiResponse.success(response)
    }
}
