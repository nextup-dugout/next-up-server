package com.nextup.infrastructure.security.expression

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 팀 권한 검증을 위한 커스텀 보안 표현식
 *
 * @PreAuthorize("@teamSecurity.isOwner(#teamId, authentication.principal)") 형태로 사용합니다.
 * authentication.principal은 JwtAuthenticationFilter에서 Long(userId)로 설정됩니다.
 *
 * Caffeine 캐시를 사용하여 팀 멤버 역할 조회를 TTL 5분으로 캐싱합니다.
 */
@Component("teamSecurity")
class TeamSecurityExpression(
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    private val memberRoleCache: Cache<String, TeamMemberRole> =
        Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build()

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
        val role = getMemberRole(teamId, userId) ?: return false
        return role == TeamMemberRole.OWNER
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
        val role = getMemberRole(teamId, userId) ?: return false
        return role == TeamMemberRole.OWNER || role == TeamMemberRole.MANAGER
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
        return getMemberRole(teamId, userId) != null
    }

    /**
     * 역할 변경 시 캐시를 무효화합니다.
     *
     * @param teamId 팀 ID
     * @param userId 사용자 ID
     */
    fun evict(
        teamId: Long,
        userId: Long,
    ) {
        memberRoleCache.invalidate("$teamId:$userId")
    }

    private fun getMemberRole(
        teamId: Long,
        userId: Long,
    ): TeamMemberRole? {
        val key = "$teamId:$userId"
        memberRoleCache.getIfPresent(key)?.let { return it }
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId) ?: return null
        memberRoleCache.put(key, member.role)
        return member.role
    }

    private fun extractUserId(principal: Any?): Long? =
        when (principal) {
            is Long -> principal
            is Number -> principal.toLong()
            is String -> principal.toLongOrNull()
            else -> null
        }
}
