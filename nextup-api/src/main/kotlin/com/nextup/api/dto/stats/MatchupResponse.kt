package com.nextup.api.dto.stats

import java.math.BigDecimal

data class MatchupResponse(
    val pitcherId: Long,
    val pitcherName: String,
    val batterId: Long,
    val batterName: String,
    val year: Int?,
    val stats: MatchupStatsResponse,
    val history: List<MatchupHistoryResponse>,
)

data class MatchupStatsResponse(
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

data class MatchupHistoryResponse(
    val gameId: Long,
    val gameDate: String,
    val result: String,
    val description: String,
)
