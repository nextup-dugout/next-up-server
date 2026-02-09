package com.nextup.backoffice.dto.discipline

import com.nextup.core.domain.discipline.DisciplineType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 징계 발급 요청 DTO (관리자용)
 */
data class IssueDisciplineRequest(
    @field:NotNull(message = "선수 ID는 필수입니다")
    @field:Positive(message = "선수 ID는 양수여야 합니다")
    val playerId: Long,
    @field:NotNull(message = "대회 ID는 필수입니다")
    @field:Positive(message = "대회 ID는 양수여야 합니다")
    val competitionId: Long,
    @field:NotNull(message = "징계 유형은 필수입니다")
    val type: DisciplineType,
    @field:NotBlank(message = "징계 사유는 필수입니다")
    @field:Size(max = 1000, message = "징계 사유는 1000자를 초과할 수 없습니다")
    val reason: String,
    val suspensionGames: Int? = null,
    val expiresAt: LocalDateTime? = null,
    @field:NotBlank(message = "발급자는 필수입니다")
    @field:Size(max = 255, message = "발급자는 255자를 초과할 수 없습니다")
    val issuedBy: String,
)
