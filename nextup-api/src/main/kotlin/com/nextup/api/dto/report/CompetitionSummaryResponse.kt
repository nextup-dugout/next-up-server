package com.nextup.api.dto.report

/**
 * 대회 요약 응답 DTO
 */
data class CompetitionSummaryResponse(
    val competitionId: Long,
    val totalGames: Int,
    val completedGames: Int,
    val totalRuns: Int,
    val averageRunsPerGame: Double,
    val totalHits: Int,
    val totalHomeRuns: Int,
    val totalStrikeouts: Int,
    val highestScoringGame: HighScoringGameResponse?,
    val longestWinStreak: WinStreakResponse?,
)
