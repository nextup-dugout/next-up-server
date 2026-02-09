package com.nextup.api.dto.match

import com.nextup.core.domain.match.SkillLevel
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateMatchRequestApiRequest(
    @field:NotNull(message = "팀 ID는 필수입니다")
    val teamId: Long,
    @field:NotNull(message = "선호 날짜는 필수입니다")
    val preferredDate: LocalDate,
    val preferredTime: String?,
    val preferredLocation: String?,
    val message: String?,
    @field:NotNull(message = "실력 수준은 필수입니다")
    val skillLevel: SkillLevel,
)
