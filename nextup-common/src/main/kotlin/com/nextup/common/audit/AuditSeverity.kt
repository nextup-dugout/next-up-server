package com.nextup.common.audit

/**
 * 감사 로그 심각도 레벨
 *
 * OWASP A09 (Security Logging and Monitoring Failures) 준수를 위한 심각도 분류
 */
enum class AuditSeverity {
    /** 일반적인 감사 이벤트 (조회, 생성 등) */
    INFO,

    /** 주의가 필요한 이벤트 (역할 변경, 강제 탈퇴 등) */
    WARN,

    /** 즉각적인 조치가 필요한 이벤트 (보안 위반, 권한 남용 등) */
    ERROR,
}
