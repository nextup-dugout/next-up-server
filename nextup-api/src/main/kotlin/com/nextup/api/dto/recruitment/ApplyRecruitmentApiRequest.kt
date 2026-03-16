package com.nextup.api.dto.recruitment

import jakarta.validation.constraints.NotBlank

/**
 * 모집 공고 지원 요청 DTO (API)
 */
data class ApplyRecruitmentApiRequest(
    @field:NotBlank(message = "지원 메시지는 필수입니다")
    val message: String,
    @field:NotBlank(message = "선호 포지션은 필수입니다")
    val preferredPositions: String,
)
