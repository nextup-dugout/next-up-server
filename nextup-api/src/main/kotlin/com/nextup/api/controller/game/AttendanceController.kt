package com.nextup.api.controller.game

import com.nextup.api.dto.attendance.*
import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.mapper.attendance.toMemberVoteResponse
import com.nextup.api.mapper.attendance.toResponse
import com.nextup.core.service.game.GameParticipationService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 출석 투표 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/games/{gameId}/attendance")
class AttendanceController(
    private val attendanceService: GameParticipationService,
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
        // 현재 사용자의 멤버 ID 조회
        val member = attendanceService.findMemberInGame(gameId, userId)

        val vote =
            attendanceService.vote(
                gameId = gameId,
                memberId = member.id,
                status = request.status,
                absenceReason = request.absenceReason,
                reasonDetail = request.reasonDetail,
            )

        return ApiResponse.success(vote.toResponse())
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
        // 인가 검증: 경기 참가 팀의 멤버인지 확인
        attendanceService.verifyGameTeamMember(gameId, userId)

        val gameDate = attendanceService.getGameScheduledAt(gameId)
        val votes = attendanceService.getVotesByGameId(gameId)
        val summary = attendanceService.getVoteSummary(gameId)

        val response =
            AttendanceVotesResponse(
                gameId = gameId,
                gameDate = gameDate,
                votes = votes.toMemberVoteResponse(),
                summary = summary.toResponse(),
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
        // 인가 검증: 경기 참가 팀의 멤버인지 확인
        attendanceService.verifyGameTeamMember(gameId, userId)

        val summary = attendanceService.getVoteSummary(gameId)
        return ApiResponse.success(summary.toResponse())
    }

    /**
     * 미투표자 목록을 조회합니다.
     * 해당 경기에 참가하는 팀의 멤버만 조회 가능합니다.
     */
    @GetMapping("/non-voters")
    fun getNonVoters(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<MemberSummary>> {
        // 인가 검증: 경기 참가 팀의 멤버인지 확인
        attendanceService.verifyGameTeamMember(gameId, userId)

        val nonVoters = attendanceService.getNonVoters(gameId)
        val response =
            nonVoters.map {
                MemberSummary(
                    memberId = it.id,
                    nickname = it.user.nickname,
                    uniformNumber = it.uniformNumber,
                    position = it.player.primaryPosition.abbreviation,
                )
            }
        return ApiResponse.success(response)
    }
}
