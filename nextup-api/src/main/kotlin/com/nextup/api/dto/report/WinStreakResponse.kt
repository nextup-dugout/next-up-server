package com.nextup.api.dto.report

/**
 * 연승 기록 응답 DTO
 */
data class WinStreakResponse(
    val teamName: String,
    val streakLength: Int,
)
