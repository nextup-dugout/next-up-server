package com.nextup.backoffice.controller.team

import com.nextup.backoffice.dto.common.ApiResponse
import com.nextup.backoffice.dto.team.*
import com.nextup.common.exception.TeamMemberNotFoundException
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.team.TeamMembershipService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

/**
 * 팀 멤버 관리 API Controller (관리자용)
 *
 * 관리자가 팀 멤버를 관리하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/backoffice/teams/{teamId}/members")
class TeamMemberAdminController(
    private val teamMembershipService: TeamMembershipService,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    /**
     * 팀의 모든 멤버 목록을 조회합니다 (상태 필터 지원).
     */
    @GetMapping
    fun getAllMembers(
        @PathVariable teamId: Long,
        @RequestParam(required = false) status: TeamMemberStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<TeamMemberAdminResponse>> {
        val members =
            if (status != null) {
                val memberList = teamMemberRepository.findByTeamIdAndStatus(teamId, status)
                // 간단한 페이징 처리 (실제로는 Repository에서 페이징 지원 필요)
                val start = pageable.offset.toInt()
                val end = minOf(start + pageable.pageSize, memberList.size)
                val pageContent = if (start < memberList.size) memberList.subList(start, end) else emptyList()
                org.springframework.data.domain.PageImpl(
                    pageContent,
                    pageable,
                    memberList.size.toLong(),
                )
            } else {
                teamMemberRepository.findByTeamIdWithUserAndPlayer(teamId, pageable)
            }

        return ApiResponse.success(members.map { TeamMemberAdminResponse.from(it) })
    }

    /**
     * 특정 멤버의 상태를 변경합니다.
     */
    @PatchMapping("/{memberId}/status")
    fun updateMemberStatus(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: UpdateMemberStatusRequest,
    ): ApiResponse<TeamMemberAdminResponse> {
        val member =
            teamMemberRepository.findByIdOrNull(memberId)
                ?: throw TeamMemberNotFoundException(memberId)

        // 상태 변경 (관리자 권한으로 직접 변경)
        when (request.status) {
            TeamMemberStatus.ACTIVE -> {
                if (member.status == TeamMemberStatus.SUSPENDED) {
                    // 활동 재개 (OWNER 권한이 필요하므로 우회)
                    member.status = TeamMemberStatus.ACTIVE
                }
            }
            TeamMemberStatus.SUSPENDED -> {
                // 활동 정지
                member.status = TeamMemberStatus.SUSPENDED
                member.memo = request.reason
            }
            else -> {
                throw com.nextup.common.exception.InvalidInputException(
                    "INVALID_STATUS",
                    "Cannot change to status: ${request.status}",
                )
            }
        }

        val updated = teamMemberRepository.save(member)
        return ApiResponse.success(TeamMemberAdminResponse.from(updated))
    }

    /**
     * 멤버를 완전히 삭제합니다 (관리자 전용).
     */
    @DeleteMapping("/{memberId}")
    fun deleteMember(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
    ): ApiResponse<Unit> {
        teamMemberRepository.deleteById(memberId)
        return ApiResponse.success(Unit)
    }
}
