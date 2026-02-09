package com.nextup.backoffice.dto.stadium

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * 구장 슬롯 생성 요청 DTO (백오피스용)
 */
data class CreateSlotRequest(
    @field:NotNull(message = "Stadium ID is required")
    @field:Positive(message = "Stadium ID must be positive")
    val stadiumId: Long,
    @field:NotNull(message = "Date is required")
    val date: LocalDate,
    @field:NotNull(message = "Start time is required")
    val startTime: LocalTime,
    @field:NotNull(message = "End time is required")
    val endTime: LocalTime,
    @field:Positive(message = "Price must be positive")
    val price: BigDecimal? = null,
)
