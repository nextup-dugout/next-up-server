package com.nextup.backoffice.dto.team

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * 팀 멤버 관리자용 응답 DTO
 */
data class TeamMemberAdminResponse(
    val memberId: Long,
    val teamId: Long,
    val teamName: String,
    val userId: Long,
    val userNickname: String,
    val userEmail: String,
    val playerId: Long,
    val playerName: String,
    val role: TeamMemberRole,
    val uniformNumber: Int,
    val status: TeamMemberStatus,
    val joinedAt: LocalDateTime,
    val leftAt: LocalDateTime?,
    val memo: String?,
) {
    companion object {
        fun from(member: TeamMember): TeamMemberAdminResponse =
            TeamMemberAdminResponse(
                memberId = member.id,
                teamId = member.team.id,
                teamName = member.team.name,
                userId = member.user.id,
                userNickname = member.user.nickname,
                userEmail = member.user.email,
                playerId = member.player.id,
                playerName = member.player.name,
                role = member.role,
                uniformNumber = member.uniformNumber,
                status = member.status,
                joinedAt = member.joinedAt,
                leftAt = member.leftAt,
                memo = member.memo,
            )
    }
}

/**
 * 멤버 상태 변경 요청 DTO
 */
data class UpdateMemberStatusRequest(
    @field:NotNull(message = "상태는 필수입니다")
    val status: TeamMemberStatus,
    val reason: String? = null,
)
