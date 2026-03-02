package com.nextup.core.port.repository

import com.nextup.core.domain.audit.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant

interface AuditLogRepositoryPort {
    fun save(auditLog: AuditLog): AuditLog

    fun findByAdminUserId(adminUserId: Long): List<AuditLog>

    fun findByTargetEntityAndTargetId(
        targetEntity: String,
        targetId: Long,
    ): List<AuditLog>

    fun findAuditLogById(id: Long): AuditLog?

    fun findAllByCondition(
        adminUserId: Long?,
        action: String?,
        targetEntity: String?,
        fromDate: Instant?,
        toDate: Instant?,
        pageable: Pageable,
    ): Page<AuditLog>
}
