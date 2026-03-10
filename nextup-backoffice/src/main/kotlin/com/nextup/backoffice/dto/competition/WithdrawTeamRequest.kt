package com.nextup.backoffice.dto.competition

import jakarta.validation.constraints.NotBlank

/**
 * 팀 대회 탈퇴 요청 DTO
 */
data class WithdrawTeamRequest(
    @field:NotBlank(message = "탈퇴 사유는 필수입니다")
    val reason: String,
)
