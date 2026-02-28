package com.nextup.infrastructure.security.expression

import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.springframework.stereotype.Component

/**
 * 라인업 권한 검증을 위한 커스텀 보안 표현식
 *
 * @PreAuthorize("@lineupSecurity.canSubmit(#submissionId, authentication.principal)") 형태로 사용합니다.
 * authentication.principal은 JwtAuthenticationFilter에서 Long(userId)로 설정됩니다.
 */
@Component("lineupSecurity")
class LineupSecurityExpression(
    private val lineupSubmissionRepository: LineupSubmissionRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    /**
     * 요청자가 해당 라인업 제출의 팀에서 OWNER 또는 MANAGER인지 확인합니다.
     *
     * @param submissionId 라인업 제출 ID
     * @param principal JWT 인증 principal (userId: Long)
     * @return OWNER 또는 MANAGER이면 true
     */
    fun canSubmit(
        submissionId: Long,
        principal: Any?,
    ): Boolean {
        val userId = extractUserId(principal) ?: return false
        val submission = lineupSubmissionRepository.findByIdOrNull(submissionId) ?: return false
        val teamId = submission.team.id
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId) ?: return false
        return member.canManageMembers()
    }

    private fun extractUserId(principal: Any?): Long? =
        when (principal) {
            is Long -> principal
            is Number -> principal.toLong()
            is String -> principal.toLongOrNull()
            else -> null
        }
}
