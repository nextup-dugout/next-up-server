package com.nextup.infrastructure.aspect

import com.nextup.common.audit.AuditLog
import com.nextup.common.audit.AuditSeverity
import com.nextup.infrastructure.security.userdetails.CustomUserDetails
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant

/**
 * AOP 기반 보안 이벤트 감사 로그 Aspect
 *
 * @AuditLog 어노테이션이 적용된 메서드 실행 시 구조화된 감사 로그를 기록합니다.
 * OWASP A09 (Security Logging and Monitoring Failures) 준수
 *
 * 로그 포맷:
 * [AUDIT] timestamp=... action=... severity=... user=... ip=... result=SUCCESS|FAILURE
 *
 * 민감 정보 처리:
 * - 비밀번호, 토큰 등 민감 정보는 로깅하지 않습니다.
 * - 파라미터는 타입명만 기록합니다.
 */
@Aspect
@Component
class AuditLogAspect {
    private val log = LoggerFactory.getLogger(AuditLogAspect::class.java)

    @Around("@annotation(auditLog)")
    fun audit(
        joinPoint: ProceedingJoinPoint,
        auditLog: AuditLog
    ): Any? {
        val timestamp = Instant.now()
        val action = auditLog.action
        val severity = auditLog.severity
        val userId = resolveUserId()
        val ip = resolveClientIp()

        return try {
            val result = joinPoint.proceed()
            logAuditEvent(
                timestamp = timestamp,
                action = action,
                severity = severity,
                userId = userId,
                ip = ip,
                result = "SUCCESS",
                errorMessage = null,
            )
            result
        } catch (ex: Exception) {
            logAuditEvent(
                timestamp = timestamp,
                action = action,
                severity = AuditSeverity.ERROR,
                userId = userId,
                ip = ip,
                result = "FAILURE",
                errorMessage = ex.javaClass.simpleName,
            )
            throw ex
        }
    }

    private fun logAuditEvent(
        timestamp: Instant,
        action: String,
        severity: AuditSeverity,
        userId: String,
        ip: String,
        result: String,
        errorMessage: String?,
    ) {
        val message =
            buildString {
                append("[AUDIT]")
                append(" timestamp=$timestamp")
                append(" action=$action")
                append(" severity=$severity")
                append(" user=$userId")
                append(" ip=$ip")
                append(" result=$result")
                if (errorMessage != null) {
                    append(" error=$errorMessage")
                }
            }

        when (severity) {
            AuditSeverity.INFO -> log.info(message)
            AuditSeverity.WARN -> log.warn(message)
            AuditSeverity.ERROR -> log.error(message)
        }
    }

    private fun resolveUserId(): String {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: return ANONYMOUS_USER

        if (!authentication.isAuthenticated) return ANONYMOUS_USER

        return when (val principal = authentication.principal) {
            is CustomUserDetails -> principal.id.toString()
            is String -> principal
            else -> authentication.name ?: ANONYMOUS_USER
        }
    }

    private fun resolveClientIp(): String {
        val requestAttributes =
            RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
                ?: return UNKNOWN_IP

        val request = requestAttributes.request

        // X-Forwarded-For 헤더 우선 확인 (프록시/로드밸런서 환경)
        val forwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR)
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.split(",").first().trim()
        }

        // X-Real-IP 헤더 확인 (Nginx 프록시 환경)
        val realIp = request.getHeader(HEADER_X_REAL_IP)
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }

        return request.remoteAddr ?: UNKNOWN_IP
    }

    companion object {
        private const val ANONYMOUS_USER = "anonymous"
        private const val UNKNOWN_IP = "unknown"
        private const val HEADER_X_FORWARDED_FOR = "X-Forwarded-For"
        private const val HEADER_X_REAL_IP = "X-Real-IP"
    }
}
