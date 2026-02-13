package com.nextup.backoffice.dto.competition

import com.nextup.core.domain.competition.CompetitionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * 대회 생성 요청 DTO (관리자용)
 */
data class CreateCompetitionRequest(
    @field:NotNull(message = "리그 ID는 필수입니다")
    @field:Positive(message = "리그 ID는 양수여야 합니다")
    val leagueId: Long,
    @field:NotBlank(message = "대회 이름은 필수입니다")
    @field:Size(max = 100, message = "대회 이름은 100자를 초과할 수 없습니다")
    val name: String,
    @field:NotNull(message = "연도는 필수입니다")
    val year: Int,
    val season: Int = 1,
    @field:NotNull(message = "대회 타입은 필수입니다")
    val type: CompetitionType,
    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    @field:Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    val description: String? = null,
    val maxTeams: Int? = null,
)

/**
 * 대회 수정 요청 DTO (관리자용)
 */
data class UpdateCompetitionRequest(
    @field:Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    val description: String? = null,
    val endDate: LocalDate? = null,
)
