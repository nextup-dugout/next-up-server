package com.nextup.core.service.stats.dto

import java.math.BigDecimal

data class MatchupDto(
    val pitcherId: Long,
    val pitcherName: String,
    val batterId: Long,
    val batterName: String,
    val year: Int?,
    val stats: MatchupStatsDto,
    val history: List<MatchupHistoryDto>,
)

data class MatchupStatsDto(
    val plateAppearances: Int,
    val atBats: Int,
    val hits: Int,
    val doubles: Int,
    val triples: Int,
    val homeRuns: Int,
    val walks: Int,
    val strikeouts: Int,
    val hitByPitch: Int,
    val sacrificeFlies: Int,
    val runsBattedIn: Int,
    val battingAverage: BigDecimal,
    val onBasePercentage: BigDecimal,
    val sluggingPercentage: BigDecimal,
)

data class MatchupHistoryDto(
    val gameId: Long,
    val gameDate: String,
    val result: String,
    val description: String,
)
