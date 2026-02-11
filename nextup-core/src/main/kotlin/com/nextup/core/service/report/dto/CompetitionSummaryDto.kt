package com.nextup.core.service.report.dto

/**
 * 대회 요약 DTO
 *
 * 대회의 주요 통계 정보를 제공합니다.
 */
data class CompetitionSummaryDto(
    val competitionId: Long,
    val totalGames: Int,
    val completedGames: Int,
    val totalRuns: Int,
    val averageRunsPerGame: Double,
    val totalHits: Int,
    val totalHomeRuns: Int,
    val totalStrikeouts: Int,
    val highestScoringGame: HighScoringGameDto?,
    val longestWinStreak: WinStreakDto?,
)
