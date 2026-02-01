package com.nextup.scorer.dto.league

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 리그 생성 요청 DTO
 */
data class CreateLeagueRequest(
    @field:NotNull(message = "협회 ID는 필수입니다")
    val associationId: Long,

    @field:NotBlank(message = "리그 이름은 필수입니다")
    @field:Size(max = 100, message = "리그 이름은 100자를 초과할 수 없습니다")
    val name: String,

    @field:Size(max = 20, message = "약어는 20자를 초과할 수 없습니다")
    val abbreviation: String? = null,

    @field:NotNull(message = "창설 연도는 필수입니다")
    @field:Min(value = 1900, message = "창설 연도는 1900년 이후여야 합니다")
    val foundedYear: Int,

    @field:Min(value = 1, message = "부 레벨은 1 이상이어야 합니다")
    val divisionLevel: Int? = null,

    @field:Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    val description: String? = null,

    @field:Size(max = 255, message = "로고 URL은 255자를 초과할 수 없습니다")
    val logoUrl: String? = null
)

/**
 * 리그 수정 요청 DTO
 */
data class UpdateLeagueRequest(
    @field:Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    val description: String? = null,

    @field:Size(max = 255, message = "로고 URL은 255자를 초과할 수 없습니다")
    val logoUrl: String? = null
)
