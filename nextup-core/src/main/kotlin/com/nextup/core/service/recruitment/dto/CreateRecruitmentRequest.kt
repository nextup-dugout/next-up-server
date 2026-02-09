package com.nextup.core.service.recruitment.dto

import java.time.LocalDate

data class CreateRecruitmentRequest(
    val teamId: Long,
    val title: String,
    val description: String,
    val positionsNeeded: String,
    val ageRange: String?,
    val skillLevel: String?,
    val location: String?,
    val deadline: LocalDate,
)
