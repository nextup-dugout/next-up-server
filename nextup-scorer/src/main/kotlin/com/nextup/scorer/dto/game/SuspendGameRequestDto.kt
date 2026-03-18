package com.nextup.scorer.dto.game

/**
 * 경기 중단 요청 DTO
 */
data class SuspendGameRequestDto(
    val reason: String? = null,
)
