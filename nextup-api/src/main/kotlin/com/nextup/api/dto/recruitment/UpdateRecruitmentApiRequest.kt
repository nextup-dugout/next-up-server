package com.nextup.api.dto.recruitment

import jakarta.validation.constraints.Future
import java.time.LocalDate

/**
 * 팀 모집 공고 수정 요청 DTO (API)
 */
data class UpdateRecruitmentApiRequest(
    val title: String?,
    val description: String?,
    val positionsNeeded: String?,
    @field:Future(message = "마감일은 현재 날짜보다 이후여야 합니다")
    val deadline: LocalDate?,
)
