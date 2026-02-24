package com.nextup.common.audit

/**
 * 보안 이벤트 감사 로그 어노테이션
 *
 * AOP 기반으로 보안에 민감한 작업을 자동으로 로깅합니다.
 * OWASP A09 (Security Logging and Monitoring Failures) 준수
 *
 * 사용 예:
 * ```kotlin
 * @AuditLog(action = "KICK_MEMBER", severity = AuditSeverity.WARN)
 * fun kickMember(memberId: Long, kickerUserId: Long, reason: String, addToBlacklist: Boolean)
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuditLog(
    /** 감사 대상 액션 명칭 (예: "KICK_MEMBER", "ROLE_CHANGE") */
    val action: String,
    /** 감사 로그 심각도 */
    val severity: AuditSeverity = AuditSeverity.INFO,
)
