package com.nextup.backoffice.dto.admin

import com.nextup.core.domain.admin.OrganizationRole
import com.nextup.core.domain.admin.OrganizationType
import java.time.Instant

/**
 * 조직 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 * 변환 로직은 OrganizationAdminExtensions.kt의 Extension Function을 사용합니다.
 */
data class OrganizationAdminResponse(
    val id: Long,
    val userId: Long,
    val userName: String?,
    val userEmail: String?,
    val organizationType: OrganizationType,
    val organizationId: Long,
    val organizationName: String?,
    val role: OrganizationRole,
    val assignedAt: Instant,
    val assignedBy: Long?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
