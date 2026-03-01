package com.nextup.core.port.repository

import com.nextup.core.domain.audit.AuditLog

interface AuditLogRepositoryPort {
    fun save(auditLog: AuditLog): AuditLog

    fun findByAdminUserId(adminUserId: Long): List<AuditLog>

    fun findByTargetEntityAndTargetId(
        targetEntity: String,
        targetId: Long,
    ): List<AuditLog>
}
