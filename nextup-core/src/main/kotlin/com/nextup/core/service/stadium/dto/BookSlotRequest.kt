package com.nextup.core.service.stadium.dto

/**
 * 구장 슬롯 예약 요청 DTO
 */
data class BookSlotRequest(
    val slotId: Long,
    val teamId: Long,
    val bookedBy: Long,
)
