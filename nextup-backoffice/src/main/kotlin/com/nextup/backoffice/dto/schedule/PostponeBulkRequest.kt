package com.nextup.backoffice.dto.schedule

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class PostponeBulkRequest(
    @field:NotNull(message = "연기 날짜는 필수입니다")
    val date: LocalDate,
    @field:NotBlank(message = "연기 사유는 필수입니다")
    val reason: String,
)
