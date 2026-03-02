package com.nextup.infrastructure.repository.audit

import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.port.repository.AuditLogRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AuditLogRepository :
    JpaRepository<AuditLog, Long>,
    AuditLogRepositoryPort {
    override fun findByAdminUserId(adminUserId: Long): List<AuditLog>

    override fun findByTargetEntityAndTargetId(
        targetEntity: String,
        targetId: Long,
    ): List<AuditLog>

    @Query("SELECT a FROM AuditLog a WHERE a.id = :id")
    override fun findAuditLogById(
        @Param("id") id: Long,
    ): AuditLog?

    @Query(
        """
        SELECT a FROM AuditLog a
        WHERE (:adminUserId IS NULL OR a.adminUserId = :adminUserId)
          AND (:action IS NULL OR a.action = :action)
          AND (:targetEntity IS NULL OR a.targetEntity = :targetEntity)
          AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
          AND (:toDate IS NULL OR a.createdAt <= :toDate)
        ORDER BY a.createdAt DESC
        """,
    )
    override fun findAllByCondition(
        @Param("adminUserId") adminUserId: Long?,
        @Param("action") action: String?,
        @Param("targetEntity") targetEntity: String?,
        @Param("fromDate") fromDate: Instant?,
        @Param("toDate") toDate: Instant?,
        pageable: Pageable,
    ): Page<AuditLog>
}
