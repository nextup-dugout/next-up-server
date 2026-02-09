package com.nextup.backoffice.dto.appeal

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 이의 제기 승인 요청 DTO (관리자용)
 */
data class ApproveAppealAdminRequest(
    @field:NotNull(message = "검토자 ID는 필수입니다")
    @field:Positive(message = "검토자 ID는 양수여야 합니다")
    val reviewerId: Long,
    val comment: String? = null,
)

/**
 * 이의 제기 반려 요청 DTO (관리자용)
 */
data class RejectAppealAdminRequest(
    @field:NotNull(message = "검토자 ID는 필수입니다")
    @field:Positive(message = "검토자 ID는 양수여야 합니다")
    val reviewerId: Long,
    @field:NotBlank(message = "반려 사유는 필수입니다")
    val comment: String,
)
