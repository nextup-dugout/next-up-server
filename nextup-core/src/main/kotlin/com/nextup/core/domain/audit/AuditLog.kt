package com.nextup.core.domain.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "audit_logs")
class AuditLog private constructor(
    @Column(name = "admin_user_id", nullable = false)
    val adminUserId: Long,
    @Column(name = "action", nullable = false, length = 100)
    val action: String,
    @Column(name = "target_entity", nullable = false, length = 100)
    val targetEntity: String,
    @Column(name = "target_id")
    val targetId: Long?,
    @Column(name = "details", columnDefinition = "TEXT")
    val details: String?,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    companion object {
        fun create(
            adminUserId: Long,
            action: String,
            targetEntity: String,
            targetId: Long? = null,
            details: String? = null,
        ): AuditLog =
            AuditLog(
                adminUserId = adminUserId,
                action = action,
                targetEntity = targetEntity,
                targetId = targetId,
                details = details,
            )
    }
}
