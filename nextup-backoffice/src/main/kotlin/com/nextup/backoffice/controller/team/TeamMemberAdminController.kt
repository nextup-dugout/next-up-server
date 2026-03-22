package com.nextup.backoffice.controller.team

import com.nextup.backoffice.dto.team.TeamMemberAdminResponse
import com.nextup.backoffice.dto.team.UpdateMemberStatusRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.common.PageResult
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.service.audit.AuditService
import com.nextup.core.service.team.TeamMembershipService
import com.nextup.infrastructure.common.toPageCommand
import com.nextup.infrastructure.security.userdetails.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice/teams/{teamId}/members")
class TeamMemberAdminController(
    private val teamMembershipService: TeamMembershipService,
    private val auditService: AuditService,
) {
    @GetMapping
    fun getAllMembers(
        @PathVariable teamId: Long,
        @RequestParam(required = false) status: TeamMemberStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<PageResult<TeamMemberAdminResponse>> {
        val pageCommand = pageable.toPageCommand()
        val members = teamMembershipService.getMembersByTeamIdPaged(teamId, status, pageCommand)
        return ApiResponse.success(members.map { TeamMemberAdminResponse.from(it) })
    }

    @PatchMapping("/{memberId}/status")
    fun updateMemberStatus(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: UpdateMemberStatusRequest,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<TeamMemberAdminResponse> {
        val updated =
            teamMembershipService.updateMemberStatus(
                memberId = memberId,
                status = request.status,
                reason = request.reason,
            )
        auditService.log(
            adminUserId = admin.id,
            action = "UPDATE_MEMBER_STATUS",
            targetEntity = "TeamMember",
            targetId = memberId,
            details = "{\"teamId\":$teamId,\"status\":\"${request.status}\"}",
        )
        return ApiResponse.success(TeamMemberAdminResponse.from(updated))
    }

    @DeleteMapping("/{memberId}")
    fun deleteMember(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<Unit> {
        teamMembershipService.deleteMemberByAdmin(memberId)
        auditService.log(
            adminUserId = admin.id,
            action = "DELETE_MEMBER",
            targetEntity = "TeamMember",
            targetId = memberId,
            details = "{\"teamId\":$teamId}",
        )
        return ApiResponse.success(Unit)
    }
}
