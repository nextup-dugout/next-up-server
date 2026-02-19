package com.nextup.api.controller.attendance

import com.nextup.api.dto.attendance.*
import com.nextup.api.dto.common.ApiResponse
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.service.attendance.AttendanceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.format.DateTimeFormatter

/**
 * 팀 출석 투표 관리 컨트롤러
 *
 * 팀의 출석 투표 생성 및 투표 응답을 관리합니다.
 */
@RestController
@RequestMapping("/api/v1/teams/{teamId}/attendance-polls")
class TeamAttendancePollController(
    private val attendanceService: AttendanceService,
) {
    /**
     * 출석 투표를 생성합니다.
     */
    @PostMapping
    fun createPoll(
        @PathVariable teamId: Long,
        @RequestBody @Valid request: CreatePollRequest,
    ): ResponseEntity<ApiResponse<PollResponse>> {
        val poll =
            attendanceService.createPoll(
                teamId = teamId,
                title = request.title,
                eventDate = request.eventDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                deadline = request.deadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(poll.toResponse()))
    }

    /**
     * 팀의 출석 투표 목록을 조회합니다.
     */
    @GetMapping
    fun listPolls(
        @PathVariable teamId: Long,
        @RequestParam(required = false) status: PollStatus?,
    ): ApiResponse<List<PollResponse>> {
        val polls = attendanceService.listPolls(teamId, status)
        return ApiResponse.success(polls.map { it.toResponse() })
    }

    /**
     * 출석 투표를 조회합니다.
     */
    @GetMapping("/{pollId}")
    fun getPoll(
        @PathVariable teamId: Long,
        @PathVariable pollId: Long,
    ): ApiResponse<PollResponse> {
        val poll = attendanceService.getPoll(pollId)
        return ApiResponse.success(poll.toResponse())
    }

    /**
     * 출석 투표에 응답합니다.
     */
    @PutMapping("/{pollId}/vote")
    fun submitVote(
        @PathVariable teamId: Long,
        @PathVariable pollId: Long,
        @RequestBody @Valid request: SubmitVoteRequest,
    ): ApiResponse<VoteResponse> {
        val vote =
            attendanceService.submitVote(
                pollId = pollId,
                playerId = request.playerId,
                voteType = request.voteType,
                absenceReason = request.absenceReason,
                reasonDetail = request.reasonDetail,
            )

        return ApiResponse.success(vote.toResponse())
    }

    /**
     * 출석 투표를 마감합니다.
     */
    @PutMapping("/{pollId}/close")
    fun closePoll(
        @PathVariable teamId: Long,
        @PathVariable pollId: Long,
    ): ApiResponse<PollResponse> {
        val poll = attendanceService.closePoll(pollId)
        return ApiResponse.success(poll.toResponse())
    }

    /**
     * 출석 투표의 응답 목록을 조회합니다.
     */
    @GetMapping("/{pollId}/votes")
    fun listVotes(
        @PathVariable teamId: Long,
        @PathVariable pollId: Long,
    ): ApiResponse<List<VoteResponse>> {
        val votes = attendanceService.listVotes(pollId)
        return ApiResponse.success(votes.map { it.toResponse() })
    }

    /**
     * 출석 투표 통계를 조회합니다.
     */
    @GetMapping("/{pollId}/statistics")
    fun getPollStatistics(
        @PathVariable teamId: Long,
        @PathVariable pollId: Long,
    ): ApiResponse<PollStatisticsResponse> {
        val votes = attendanceService.listVotes(pollId)
        return ApiResponse.success(votes.toStatistics(pollId))
    }
}
