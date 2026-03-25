package com.nextup.backoffice.dto.audit

import com.nextup.core.domain.audit.AuditLog

/**
 * AuditLog Entity를 AuditLogResponse DTO로 변환하는 Extension Function
 */
fun AuditLog.toResponse(): AuditLogResponse =
    AuditLogResponse(
        id = this.id!!,
        adminUserId = this.adminUserId,
        action = this.action,
        targetEntity = this.targetEntity,
        targetId = this.targetId,
        details = this.details,
        createdAt = this.createdAt,
    )
