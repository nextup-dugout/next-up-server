package com.nextup.infrastructure.security.expression

import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.springframework.stereotype.Component

/**
 * 팀 권한 검증을 위한 커스텀 보안 표현식
 *
 * @PreAuthorize("@teamSecurity.isOwner(#teamId, authentication.principal)") 형태로 사용합니다.
 * authentication.principal은 JwtAuthenticationFilter에서 Long(userId)로 설정됩니다.
 */
@Component("teamSecurity")
class TeamSecurityExpression(
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    /**
     * 요청자가 해당 팀의 OWNER인지 확인합니다.
     *
     * @param teamId 팀 ID
     * @param principal JWT 인증 principal (userId: Long)
     * @return OWNER이면 true
     */
    fun isOwner(
        teamId: Long,
        principal: Any?,
    ): Boolean {
        val userId = extractUserId(principal) ?: return false
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId) ?: return false
        return member.role == TeamMemberRole.OWNER
    }

    /**
     * 요청자가 해당 팀의 OWNER 또는 MANAGER인지 확인합니다.
     *
     * @param teamId 팀 ID
     * @param principal JWT 인증 principal (userId: Long)
     * @return OWNER 또는 MANAGER이면 true
     */
    fun isOwnerOrManager(
        teamId: Long,
        principal: Any?,
    ): Boolean {
        val userId = extractUserId(principal) ?: return false
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId) ?: return false
        return member.role == TeamMemberRole.OWNER || member.role == TeamMemberRole.MANAGER
    }

    /**
     * 요청자가 해당 팀의 활성 멤버인지 확인합니다.
     *
     * @param teamId 팀 ID
     * @param principal JWT 인증 principal (userId: Long)
     * @return 활성 멤버이면 true
     */
    fun isMember(
        teamId: Long,
        principal: Any?,
    ): Boolean {
        val userId = extractUserId(principal) ?: return false
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId) != null
    }

    private fun extractUserId(principal: Any?): Long? =
        when (principal) {
            is Long -> principal
            is Number -> principal.toLong()
            is String -> principal.toLongOrNull()
            else -> null
        }
}
