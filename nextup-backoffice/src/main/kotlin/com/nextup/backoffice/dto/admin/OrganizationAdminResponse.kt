package com.nextup.backoffice.dto.admin

import com.nextup.core.domain.admin.OrganizationAdmin
import com.nextup.core.domain.admin.OrganizationRole
import com.nextup.core.domain.admin.OrganizationType
import java.time.Instant

/**
 * 조직 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
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
    val updatedAt: Instant
) {
    companion object {
        /**
         * OrganizationAdmin Entity로부터 응답 DTO를 생성합니다.
         *
         * organizationName은 별도 조회가 필요하므로 null로 설정됩니다.
         * 필요한 경우 Service 레이어에서 추가 조회하여 설정해야 합니다.
         */
        fun from(admin: OrganizationAdmin): OrganizationAdminResponse {
            return OrganizationAdminResponse(
                id = admin.id,
                userId = admin.user.id,
                userName = admin.user.nickname,
                userEmail = admin.user.email,
                organizationType = admin.organizationType,
                organizationId = admin.organizationId,
                organizationName = null, // 별도 조회 필요
                role = admin.role,
                assignedAt = admin.assignedAt,
                assignedBy = admin.assignedBy,
                isActive = admin.isActive,
                createdAt = admin.createdAt,
                updatedAt = admin.updatedAt
            )
        }

        /**
         * OrganizationAdmin Entity와 조직 이름으로부터 응답 DTO를 생성합니다.
         */
        fun from(admin: OrganizationAdmin, organizationName: String?): OrganizationAdminResponse {
            return OrganizationAdminResponse(
                id = admin.id,
                userId = admin.user.id,
                userName = admin.user.nickname,
                userEmail = admin.user.email,
                organizationType = admin.organizationType,
                organizationId = admin.organizationId,
                organizationName = organizationName,
                role = admin.role,
                assignedAt = admin.assignedAt,
                assignedBy = admin.assignedBy,
                isActive = admin.isActive,
                createdAt = admin.createdAt,
                updatedAt = admin.updatedAt
            )
        }
    }
}
