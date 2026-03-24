package com.nextup.backoffice.dto.admin

import com.nextup.core.domain.admin.OrganizationAdmin

/**
 * OrganizationAdmin Entity를 OrganizationAdminResponse DTO로 변환하는 Extension Function
 *
 * organizationName은 별도 조회가 필요하므로 null로 설정됩니다.
 * 필요한 경우 Service 레이어에서 추가 조회하여 설정해야 합니다.
 */
fun OrganizationAdmin.toResponse(): OrganizationAdminResponse =
    OrganizationAdminResponse(
        id = this.id,
        userId = this.user.id,
        userName = this.user.nickname,
        userEmail = this.user.email,
        organizationType = this.organizationType,
        organizationId = this.organizationId,
        organizationName = null, // 별도 조회 필요
        role = this.role,
        assignedAt = this.assignedAt,
        assignedBy = this.assignedBy,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

/**
 * OrganizationAdmin Entity와 조직 이름으로부터 응답 DTO를 생성하는 Extension Function
 */
fun OrganizationAdmin.toResponse(organizationName: String?): OrganizationAdminResponse =
    OrganizationAdminResponse(
        id = this.id,
        userId = this.user.id,
        userName = this.user.nickname,
        userEmail = this.user.email,
        organizationType = this.organizationType,
        organizationId = this.organizationId,
        organizationName = organizationName,
        role = this.role,
        assignedAt = this.assignedAt,
        assignedBy = this.assignedBy,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
