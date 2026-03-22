package com.nextup.infrastructure.aspect

import com.nextup.core.annotation.RequiresTeamRole
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * @RequiresTeamRole 어노테이션을 처리하는 AOP Aspect
 *
 * Controller 메서드에 `@RequiresTeamRole`이 적용되면:
 * 1. SecurityContext에서 인증된 userId를 추출합니다.
 * 2. 메서드 파라미터에서 teamId를 식별합니다.
 * 3. 해당 팀에서 사용자의 역할을 조회합니다.
 * 4. 역할이 어노테이션에 명시된 허용 목록에 포함되지 않으면 AccessDeniedException을 발생시킵니다.
 */
@Aspect
@Component
class RequiresTeamRoleAspect(
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(RequiresTeamRoleAspect::class.java)

    @Around("@annotation(requiresTeamRole)")
    fun checkTeamRole(
        joinPoint: ProceedingJoinPoint,
        requiresTeamRole: RequiresTeamRole,
    ): Any? {
        val userId = extractUserId()
        val teamId = extractTeamId(joinPoint, requiresTeamRole.teamIdParam)

        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
        if (member == null || member.role !in requiresTeamRole.roles) {
            val allowedRoles = requiresTeamRole.roles.joinToString(", ") { it.name }
            logger.warn(
                "팀 역할 인가 실패 (userId={}, teamId={}, actualRole={}, requiredRoles=[{}])",
                userId,
                teamId,
                member?.role?.name ?: "NONE",
                allowedRoles,
            )
            throw AccessDeniedException(
                "팀 역할 권한 부족: 요구 역할=[$allowedRoles], 실제 역할=${member?.role?.name ?: "미소속"}",
            )
        }

        logger.debug(
            "팀 역할 인가 성공 (userId={}, teamId={}, role={})",
            userId,
            teamId,
            member.role.name,
        )

        return joinPoint.proceed()
    }

    private fun extractUserId(): Long {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("인증 정보가 없습니다.")

        if (!authentication.isAuthenticated) {
            throw AccessDeniedException("인증되지 않은 요청입니다.")
        }

        return when (val principal = authentication.principal) {
            is Long -> principal
            is Number -> principal.toLong()
            is String ->
                principal.toLongOrNull()
                    ?: throw AccessDeniedException("유효하지 않은 인증 정보입니다.")
            else -> throw AccessDeniedException("유효하지 않은 인증 정보입니다.")
        }
    }

    private fun extractTeamId(
        joinPoint: ProceedingJoinPoint,
        teamIdParam: String,
    ): Long {
        val signature = joinPoint.signature as MethodSignature
        val parameterNames = signature.parameterNames
        val args = joinPoint.args

        val paramIndex = parameterNames.indexOf(teamIdParam)
        if (paramIndex >= 0) {
            val value = args[paramIndex]
            return convertToLong(value, teamIdParam)
        }

        // 파라미터 이름으로 찾지 못한 경우, 객체 필드에서 teamId 탐색
        for (arg in args) {
            if (arg == null) continue
            try {
                val field = arg.javaClass.getDeclaredField(teamIdParam)
                field.isAccessible = true
                val value = field.get(arg)
                if (value != null) {
                    return convertToLong(value, teamIdParam)
                }
            } catch (_: NoSuchFieldException) {
                // 해당 필드가 없으면 다음 인자 탐색
            }
        }

        throw IllegalArgumentException(
            "teamId 파라미터를 찾을 수 없습니다. " +
                "메서드 파라미터 또는 요청 객체에 '$teamIdParam' 필드가 필요합니다.",
        )
    }

    private fun convertToLong(
        value: Any?,
        paramName: String,
    ): Long =
        when (value) {
            is Long -> value
            is Number -> value.toLong()
            is String ->
                value.toLongOrNull()
                    ?: throw IllegalArgumentException("'$paramName' 값을 Long으로 변환할 수 없습니다: $value")
            else -> throw IllegalArgumentException("'$paramName' 타입이 지원되지 않습니다: ${value?.javaClass?.name}")
        }
}
