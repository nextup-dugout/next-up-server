package com.nextup.api.dto.stats

/**
 * 대회별 타격 통계 응답 DTO
 */
data class CompetitionBattingStatsResponse(
    val playerId: Long,
    val competitionId: Long,
    val gamesPlayed: Int,
    val plateAppearances: Int,
    val atBats: Int,
    val hits: Int,
    val doubles: Int,
    val triples: Int,
    val homeRuns: Int,
    val runs: Int,
    val runsBattedIn: Int,
    val walks: Int,
    val strikeouts: Int,
    val stolenBases: Int,
    val battingAverage: String,
    val onBasePercentage: String,
    val sluggingPercentage: String,
    val ops: String,
)

/**
 * 대회별 투수 통계 응답 DTO
 */
data class CompetitionPitchingStatsResponse(
    val playerId: Long,
    val competitionId: Long,
    val gamesPlayed: Int,
    val gamesStarted: Int,
    val inningsPitchedDisplay: String,
    val wins: Int,
    val losses: Int,
    val saves: Int,
    val holds: Int,
    val earnedRuns: Int,
    val hitsAllowed: Int,
    val walksAllowed: Int,
    val strikeouts: Int,
    val homeRunsAllowed: Int,
    val earnedRunAverage: String?,
    val whip: String,
)
