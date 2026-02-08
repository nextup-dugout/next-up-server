package com.nextup.api.dto.team

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 팀 생성 요청 DTO
 */
data class CreateTeamRequest(
    @field:NotBlank(message = "팀 이름은 필수입니다")
    @field:Size(max = 100, message = "팀 이름은 100자 이하여야 합니다")
    val name: String,
    @field:NotBlank(message = "도시는 필수입니다")
    @field:Size(max = 100, message = "도시는 100자 이하여야 합니다")
    val city: String,
    val leagueId: Long? = null,
    @field:Size(max = 20, message = "약어는 20자 이하여야 합니다")
    val abbreviation: String? = null,
    @field:Min(value = 1, message = "등번호는 1 이상이어야 합니다")
    @field:Max(value = 99, message = "등번호는 99 이하여야 합니다")
    val uniformNumber: Int = 1,
)

/**
 * 팀 수정 요청 DTO
 */
data class UpdateTeamRequest(
    @field:Size(max = 100, message = "팀 이름은 100자 이하여야 합니다")
    val name: String? = null,
    @field:Size(max = 100, message = "도시는 100자 이하여야 합니다")
    val city: String? = null,
    @field:Size(max = 20, message = "약어는 20자 이하여야 합니다")
    val abbreviation: String? = null,
)
