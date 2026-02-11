package com.nextup.core.service.stadium.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * 구장 슬롯 생성 요청 DTO
 */
data class CreateSlotRequest(
    val stadiumId: Long,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val price: BigDecimal? = null,
)
