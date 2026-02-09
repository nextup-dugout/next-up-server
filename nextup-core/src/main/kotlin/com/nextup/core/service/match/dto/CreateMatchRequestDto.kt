package com.nextup.core.service.match.dto

import com.nextup.core.domain.match.SkillLevel
import java.time.LocalDate

data class CreateMatchRequestDto(
    val teamId: Long,
    val preferredDate: LocalDate,
    val preferredTime: String?,
    val preferredLocation: String?,
    val message: String?,
    val skillLevel: SkillLevel,
)
