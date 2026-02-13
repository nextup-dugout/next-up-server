package com.nextup.core.service.recruitment.dto

import java.time.LocalDate

data class UpdateRecruitmentRequest(
    val title: String?,
    val description: String?,
    val positionsNeeded: String?,
    val deadline: LocalDate?,
)
