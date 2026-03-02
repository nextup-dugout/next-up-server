package com.nextup.core.service.audit

import com.nextup.core.domain.audit.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant

interface AuditLogQueryService {
    fun findAll(
        adminUserId: Long?,
        action: String?,
        targetEntity: String?,
        fromDate: Instant?,
        toDate: Instant?,
        pageable: Pageable,
    ): Page<AuditLog>

    fun findById(id: Long): AuditLog
}
