package com.nextup.common.exception

class AuditLogNotFoundException(
    id: Long,
) : NotFoundException("AUDIT_LOG_NOT_FOUND", "Audit log not found: $id")
