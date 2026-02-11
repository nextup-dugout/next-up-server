package com.nextup.core.service.report.dto

import java.time.LocalDate

/**
 * 최다 득점 경기 DTO
 */
data class HighScoringGameDto(
    val gameId: Long,
    val homeTeamName: String,
    val awayTeamName: String,
    val totalRuns: Int,
    val date: LocalDate,
)
