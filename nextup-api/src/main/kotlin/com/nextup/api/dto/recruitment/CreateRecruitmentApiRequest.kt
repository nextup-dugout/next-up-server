package com.nextup.api.dto.recruitment

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 팀 모집 공고 생성 요청 DTO (API)
 */
data class CreateRecruitmentApiRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    @field:NotBlank(message = "설명은 필수입니다")
    val description: String,
    @field:NotBlank(message = "모집 포지션은 필수입니다")
    val positionsNeeded: String,
    val ageRange: String?,
    val skillLevel: String?,
    val location: String?,
    @field:NotNull(message = "마감일은 필수입니다")
    @field:Future(message = "마감일은 현재 날짜보다 이후여야 합니다")
    val deadline: LocalDate,
)
