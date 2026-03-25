package com.nextup.backoffice.dto.audit

import java.time.Instant

/**
 * 감사 로그 응답 DTO
 *
 * 변환 로직은 AuditExtensions.kt의 Extension Function을 사용합니다.
 */
data class AuditLogResponse(
    val id: Long,
    val adminUserId: Long,
    val action: String,
    val targetEntity: String,
    val targetId: Long?,
    val details: String?,
    val createdAt: Instant,
)
