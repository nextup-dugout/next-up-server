package com.nextup.infrastructure.repository.audit

import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.port.repository.AuditLogRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository :
    JpaRepository<AuditLog, Long>,
    AuditLogRepositoryPort {
    override fun findByAdminUserId(adminUserId: Long): List<AuditLog>

    override fun findByTargetEntityAndTargetId(
        targetEntity: String,
        targetId: Long,
    ): List<AuditLog>
}
