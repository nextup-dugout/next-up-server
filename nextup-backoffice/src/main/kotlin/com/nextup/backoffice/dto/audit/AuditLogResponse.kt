package com.nextup.backoffice.dto.audit

import com.nextup.core.domain.audit.AuditLog
import java.time.Instant

data class AuditLogResponse(
    val id: Long,
    val adminUserId: Long,
    val action: String,
    val targetEntity: String,
    val targetId: Long?,
    val details: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(auditLog: AuditLog): AuditLogResponse =
            AuditLogResponse(
                id = auditLog.id!!,
                adminUserId = auditLog.adminUserId,
                action = auditLog.action,
                targetEntity = auditLog.targetEntity,
                targetId = auditLog.targetId,
                details = auditLog.details,
                createdAt = auditLog.createdAt,
            )
    }
}
