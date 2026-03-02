package com.nextup.backoffice.dto.correction

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 타격 기록 정정 요청 DTO (Backoffice API)
 */
data class CorrectBattingRecordRequest(
    @field:NotNull(message = "관리자 ID는 필수입니다")
    @field:Positive(message = "관리자 ID는 양수여야 합니다")
    val adminUserId: Long,
    @field:NotBlank(message = "정정할 필드명은 필수입니다")
    val fieldName: String,
    @field:NotBlank(message = "정정할 새로운 값은 필수입니다")
    val newValue: String,
    @field:NotBlank(message = "정정 사유는 필수입니다")
    val reason: String,
)

/**
 * 투수 기록 정정 요청 DTO (Backoffice API)
 */
data class CorrectPitchingRecordRequest(
    @field:NotNull(message = "관리자 ID는 필수입니다")
    @field:Positive(message = "관리자 ID는 양수여야 합니다")
    val adminUserId: Long,
    @field:NotBlank(message = "정정할 필드명은 필수입니다")
    val fieldName: String,
    @field:NotBlank(message = "정정할 새로운 값은 필수입니다")
    val newValue: String,
    @field:NotBlank(message = "정정 사유는 필수입니다")
    val reason: String,
)
