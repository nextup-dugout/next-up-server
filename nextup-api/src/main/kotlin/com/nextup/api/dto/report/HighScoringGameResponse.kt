package com.nextup.api.dto.report

import java.time.LocalDate

/**
 * 최다 득점 경기 응답 DTO
 */
data class HighScoringGameResponse(
    val gameId: Long,
    val homeTeamName: String,
    val awayTeamName: String,
    val totalRuns: Int,
    val date: LocalDate,
)
