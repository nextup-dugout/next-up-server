package com.nextup.api.controller.team

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.team.*
import com.nextup.api.mapper.team.toDetailResponse
import com.nextup.api.mapper.team.toResponse
import com.nextup.core.service.team.TeamMembershipService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 팀 멤버십 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/teams/{teamId}")
class TeamMembershipController(
    private val teamMembershipService: TeamMembershipService,
) {
    /**
     * 팀 가입을 신청합니다.
     */
    @PostMapping("/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun requestJoin(
        @PathVariable teamId: Long,
        @RequestBody @Valid request: JoinRequestDto,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<JoinRequestResponse> {
        val joinRequest =
            teamMembershipService.requestJoin(
                userId = userId,
                teamId = teamId,
                desiredUniformNumber = request.desiredUniformNumber,
                message = request.requestMessage,
            )
        return ApiResponse.success(joinRequest.toResponse())
    }

    /**
     * 가입 신청 목록을 조회합니다.
     */
    @GetMapping("/join-requests")
    fun getJoinRequests(
        @PathVariable teamId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<JoinRequestResponse>> {
        // TODO: 실제 구현에서는 TeamJoinRequestRepository에서 페이징 조회 필요
        // 현재는 간단한 구현으로 대체
        return ApiResponse.success(emptyList())
    }

    /**
     * 가입 신청을 승인합니다.
     */
    @PostMapping("/join-requests/{requestId}/approve")
    fun approveJoinRequest(
        @PathVariable teamId: Long,
        @PathVariable requestId: Long,
        @RequestBody @Valid request: ApproveJoinRequestDto,
        @AuthenticationPrincipal processorUserId: Long,
    ): ApiResponse<TeamMemberResponse> {
        val member =
            teamMembershipService.approveJoinRequest(
                requestId = requestId,
                processorUserId = processorUserId,
                finalUniformNumber = request.uniformNumber,
                responseMessage = request.responseMessage,
            )
        return ApiResponse.success(member.toResponse())
    }

    /**
     * 가입 신청을 거부합니다.
     */
    @PostMapping("/join-requests/{requestId}/reject")
    fun rejectJoinRequest(
        @PathVariable teamId: Long,
        @PathVariable requestId: Long,
        @RequestBody @Valid request: RejectJoinRequestDto,
        @AuthenticationPrincipal processorUserId: Long,
    ): ApiResponse<JoinRequestResponse> {
        val joinRequest =
            teamMembershipService.rejectJoinRequest(
                requestId = requestId,
                processorUserId = processorUserId,
                reason = request.responseMessage,
            )
        return ApiResponse.success(joinRequest.toResponse())
    }

    /**
     * 팀 멤버 목록을 조회합니다.
     */
    @GetMapping("/members")
    fun getMembers(
        @PathVariable teamId: Long,
    ): ApiResponse<List<TeamMemberDetailResponse>> {
        val members = teamMembershipService.getRoster(teamId)
        return ApiResponse.success(members.toDetailResponse())
    }

    /**
     * 멤버를 강퇴합니다.
     */
    @DeleteMapping("/members/{memberId}")
    fun kickMember(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @RequestBody @Valid request: KickMemberRequest,
        @AuthenticationPrincipal kickerUserId: Long,
    ): ApiResponse<Unit> {
        // IDOR 방지: 요청자가 해당 팀의 멤버인지 검증
        teamMembershipService.getMember(teamId, kickerUserId)
            ?: throw IllegalStateException("You are not a member of this team")

        // IDOR 방지: 대상 멤버가 해당 팀에 속하는지 검증
        val targetMember =
            teamMembershipService.getMemberById(memberId)
                ?: throw IllegalStateException("Member not found: $memberId")
        if (targetMember.team.id != teamId) {
            throw IllegalStateException("Member does not belong to this team")
        }

        teamMembershipService.kickMember(
            memberId = memberId,
            kickerUserId = kickerUserId,
            reason = request.reason,
            addToBlacklist = request.addToBlacklist,
        )
        return ApiResponse.success(Unit)
    }

    /**
     * 팀에서 탈퇴합니다.
     */
    @PostMapping("/leave")
    fun leaveTeam(
        @PathVariable teamId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<Unit> {
        // 현재 사용자의 멤버 ID 조회
        val member =
            teamMembershipService.getMember(teamId, userId)
                ?: throw IllegalStateException("You are not a member of this team")

        teamMembershipService.leaveMember(member.id)
        return ApiResponse.success(Unit)
    }

    /**
     * 멤버의 역할을 변경합니다.
     */
    @PatchMapping("/members/{memberId}/role")
    fun changeRole(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @RequestBody @Valid request: ChangeRoleRequest,
        @AuthenticationPrincipal changerUserId: Long,
    ): ApiResponse<TeamMemberResponse> {
        // IDOR 방지: 요청자가 해당 팀의 멤버인지 검증
        teamMembershipService.getMember(teamId, changerUserId)
            ?: throw IllegalStateException("You are not a member of this team")

        // IDOR 방지: 대상 멤버가 해당 팀에 속하는지 검증
        val targetMember =
            teamMembershipService.getMemberById(memberId)
                ?: throw IllegalStateException("Member not found: $memberId")
        if (targetMember.team.id != teamId) {
            throw IllegalStateException("Member does not belong to this team")
        }

        val member =
            teamMembershipService.changeRole(
                memberId = memberId,
                newRole = request.newRole,
                changerUserId = changerUserId,
            )
        return ApiResponse.success(member.toResponse())
    }
}
