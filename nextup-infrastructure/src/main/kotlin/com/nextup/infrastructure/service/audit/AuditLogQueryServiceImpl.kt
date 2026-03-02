package com.nextup.infrastructure.service.audit

import com.nextup.common.exception.AuditLogNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.port.repository.AuditLogRepositoryPort
import com.nextup.core.service.audit.AuditLogQueryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class AuditLogQueryServiceImpl(
    private val auditLogRepository: AuditLogRepositoryPort,
) : AuditLogQueryService {
    override fun findAll(
        adminUserId: Long?,
        action: String?,
        targetEntity: String?,
        fromDate: Instant?,
        toDate: Instant?,
        pageCommand: PageCommand,
    ): PageResult<AuditLog> =
        auditLogRepository.findAllByCondition(
            adminUserId = adminUserId,
            action = action,
            targetEntity = targetEntity,
            fromDate = fromDate,
            toDate = toDate,
            pageCommand = pageCommand,
        )

    override fun findById(id: Long): AuditLog =
        auditLogRepository.findAuditLogById(id) ?: throw AuditLogNotFoundException(id)
}
