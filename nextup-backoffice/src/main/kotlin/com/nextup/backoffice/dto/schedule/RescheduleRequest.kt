package com.nextup.backoffice.dto.schedule

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class RescheduleRequest(
    @field:NotNull(message = "새로운 날짜는 필수입니다")
    val newDate: LocalDate,
    val newVenue: String? = null,
)
