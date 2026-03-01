package com.nextup.core.service.audit

interface AuditService {
    fun log(
        adminUserId: Long,
        action: String,
        targetEntity: String,
        targetId: Long? = null,
        details: String? = null,
    )
}
