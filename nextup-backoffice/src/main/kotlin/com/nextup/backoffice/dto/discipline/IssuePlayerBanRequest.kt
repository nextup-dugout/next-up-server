package com.nextup.backoffice.dto.discipline

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * 선수 제재 발급 요청 DTO (관리자용)
 */
data class IssuePlayerBanRequest(
    @field:NotNull(message = "선수 ID는 필수입니다")
    @field:Positive(message = "선수 ID는 양수여야 합니다")
    val playerId: Long,
    @field:NotNull(message = "대회 ID는 필수입니다")
    @field:Positive(message = "대회 ID는 양수여야 합니다")
    val competitionId: Long,
    @field:NotBlank(message = "제재 사유는 필수입니다")
    @field:Size(max = 1000, message = "제재 사유는 1000자를 초과할 수 없습니다")
    val reason: String,
    @field:NotBlank(message = "발급자는 필수입니다")
    @field:Size(max = 255, message = "발급자는 255자를 초과할 수 없습니다")
    val issuedBy: String,
)
