package com.nextup.api.dto.user

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import java.time.LocalDateTime

/**
 * 내 소속 팀 응답 DTO
 *
 * 로그인한 사용자의 활성 상태 팀 멤버십 정보를 표현합니다.
 */
data class MyTeamResponse(
    val teamId: Long,
    val teamName: String,
    val teamLogoUrl: String?,
    val role: TeamMemberRole,
    val roleDisplayName: String,
    val uniformNumber: Int,
    val joinedAt: LocalDateTime,
) {
    companion object {
        fun from(teamMember: TeamMember): MyTeamResponse =
            MyTeamResponse(
                teamId = teamMember.team.id,
                teamName = teamMember.team.name,
                teamLogoUrl = teamMember.team.logoUrl,
                role = teamMember.role,
                roleDisplayName = teamMember.role.displayName,
                uniformNumber = teamMember.uniformNumber,
                joinedAt = teamMember.joinedAt,
            )
    }
}
