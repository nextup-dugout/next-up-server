package com.nextup.backoffice.dto.player

import com.nextup.core.domain.player.Position
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDate

/**
 * 선수 팀 소속 등록 요청 DTO
 */
data class RegisterAffiliationRequest(
    @field:NotNull(message = "선수 ID는 필수입니다")
    @field:Positive(message = "선수 ID는 양수여야 합니다")
    val playerId: Long,
    @field:NotNull(message = "팀 ID는 필수입니다")
    @field:Positive(message = "팀 ID는 양수여야 합니다")
    val teamId: Long,
    @field:NotNull(message = "소속 시작일은 필수입니다")
    val startDate: LocalDate,
    @field:NotNull(message = "포지션은 필수입니다")
    val position: Position,
    val uniformNumber: Int? = null,
)

/**
 * 선수 소속 종료 요청 DTO
 */
data class EndAffiliationRequest(
    @field:NotNull(message = "종료일은 필수입니다")
    val endDate: LocalDate,
)

/**
 * 등번호 변경 요청 DTO
 */
data class ChangeUniformNumberRequest(
    @field:NotNull(message = "등번호는 필수입니다")
    @field:Positive(message = "등번호는 양수여야 합니다")
    val uniformNumber: Int,
)

/**
 * 포지션 변경 요청 DTO
 */
data class ChangePositionRequest(
    @field:NotNull(message = "포지션은 필수입니다")
    val position: Position,
)
