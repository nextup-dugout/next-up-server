package com.nextup.backoffice.dto.competition

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 다음 시즌 준비 요청 DTO
 */
data class PrepareNextSeasonRequest(
    @field:NotBlank(message = "대회 이름은 필수입니다")
    val name: String,
    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val description: String? = null,
    val maxTeams: Int? = null,
)
