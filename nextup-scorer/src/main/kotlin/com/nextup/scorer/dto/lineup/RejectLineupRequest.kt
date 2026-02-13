package com.nextup.scorer.dto.lineup

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/**
 * 라인업 반려 요청 DTO
 */
data class RejectLineupRequest(
    @field:NotEmpty(message = "반려 사유는 필수입니다.")
    @field:Size(max = 500, message = "반려 사유는 500자 이하여야 합니다.")
    val reason: String,
)
