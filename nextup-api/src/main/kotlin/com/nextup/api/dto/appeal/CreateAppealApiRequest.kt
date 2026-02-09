package com.nextup.api.dto.appeal

import com.nextup.core.domain.appeal.AppealType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 이의 제기 생성 요청 DTO (API)
 */
data class CreateAppealApiRequest(
    @field:NotNull(message = "신청자 ID는 필수입니다")
    @field:Positive(message = "신청자 ID는 양수여야 합니다")
    val appealerId: Long,
    @field:NotBlank(message = "신청자 이름은 필수입니다")
    val appealerName: String,
    @field:NotNull(message = "이의 제기 유형은 필수입니다")
    val type: AppealType,
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    @field:NotBlank(message = "상세 설명은 필수입니다")
    val description: String,
)
