package com.nextup.backoffice.dto.stadium

import com.nextup.core.domain.stadium.SlotStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * 구장 슬롯 응답 DTO (백오피스용)
 *
 * 변환 로직은 StadiumExtensions.kt의 Extension Function을 사용합니다.
 */
data class StadiumSlotResponse(
    val id: Long,
    val stadiumId: Long,
    val stadiumName: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val price: BigDecimal?,
    val status: SlotStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
