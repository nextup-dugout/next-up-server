package com.nextup.core.service.stats.dto

import java.math.BigDecimal

data class TeamStatsDto(
    val teamId: Long,
    val teamName: String,
    val year: Int?,
    val competitionId: Long?,
    val competitionName: String?,
    val record: TeamRecordDto,
    val batting: TeamBattingStatsDto,
    val pitching: TeamPitchingStatsDto,
)

data class TeamRecordDto(
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winningPercentage: BigDecimal, // 승률 (승 / (승 + 패))
)

data class TeamBattingStatsDto(
    val totalAtBats: Int,
    val totalHits: Int,
    val totalHomeRuns: Int,
    val totalRunsBattedIn: Int,
    val totalRuns: Int,
    val teamBattingAverage: BigDecimal, // 팀 타율
    val teamOnBasePercentage: BigDecimal, // 팀 출루율
    val teamSluggingPercentage: BigDecimal, // 팀 장타율
)

data class TeamPitchingStatsDto(
    val totalInningsPitchedOuts: Int,
    val inningsPitchedDisplay: String, // "150.2" 형식
    val totalEarnedRuns: Int,
    val totalStrikeouts: Int,
    val totalWalksAllowed: Int,
    val teamEra: BigDecimal, // 팀 방어율
    val teamWhip: BigDecimal, // 팀 WHIP
)
