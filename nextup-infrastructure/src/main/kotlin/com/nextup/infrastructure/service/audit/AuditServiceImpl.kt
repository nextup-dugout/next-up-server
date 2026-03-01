package com.nextup.infrastructure.service.audit

import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.port.repository.AuditLogRepositoryPort
import com.nextup.core.service.audit.AuditService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuditServiceImpl(
    private val auditLogRepository: AuditLogRepositoryPort,
) : AuditService {
    override fun log(
        adminUserId: Long,
        action: String,
        targetEntity: String,
        targetId: Long?,
        details: String?,
    ) {
        val auditLog =
            AuditLog.create(
                adminUserId = adminUserId,
                action = action,
                targetEntity = targetEntity,
                targetId = targetId,
                details = details,
            )
        auditLogRepository.save(auditLog)
    }
}
