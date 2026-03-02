package com.nextup.core.service.audit

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.audit.AuditLog
import java.time.Instant

interface AuditLogQueryService {
    fun findAll(
        adminUserId: Long?,
        action: String?,
        targetEntity: String?,
        fromDate: Instant?,
        toDate: Instant?,
        pageCommand: PageCommand,
    ): PageResult<AuditLog>

    fun findById(id: Long): AuditLog
}
