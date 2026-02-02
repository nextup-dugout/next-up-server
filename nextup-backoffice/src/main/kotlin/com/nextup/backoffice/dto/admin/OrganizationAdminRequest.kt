package com.nextup.backoffice.dto.admin

import com.nextup.core.domain.admin.OrganizationRole
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 관리자 할당 요청 DTO
 */
data class AssignAdminRequest(
    @field:NotNull(message = "사용자 ID는 필수입니다")
    @field:Positive(message = "사용자 ID는 양수여야 합니다")
    val userId: Long,

    @field:NotNull(message = "역할은 필수입니다")
    val role: OrganizationRole
)

/**
 * 관리자 역할 변경 요청 DTO
 */
data class ChangeRoleRequest(
    @field:NotNull(message = "역할은 필수입니다")
    val role: OrganizationRole
)
