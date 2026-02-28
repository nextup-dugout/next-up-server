package com.nextup.core.service.stats.dto

import java.math.BigDecimal

/**
 * 선수 기록 DTO
 *
 * 선수의 타격/투수 기록을 통합하여 제공합니다.
 */
data class PlayerRecordDto(
    val playerId: Long,
    val playerName: String,
    val scope: RecordScope,
    val type: RecordType,
    val year: Int?,
    val competitionId: Long?,
    val competitionName: String?,
    val battingStats: BattingStatsDto?,
    val pitchingStats: PitchingStatsDto?,
)

/**
 * 타격 통계 DTO
 */
data class BattingStatsDto(
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
    val battingAverage: BigDecimal,
    val onBasePercentage: BigDecimal,
    val sluggingPercentage: BigDecimal,
    val ops: BigDecimal,
)

/**
 * 투수 통계 DTO
 */
data class PitchingStatsDto(
    val gamesPlayed: Int,
    val gamesStarted: Int,
    val inningsPitched: String,
    val wins: Int,
    val losses: Int,
    val saves: Int,
    val holds: Int,
    val earnedRuns: Int,
    val hitsAllowed: Int,
    val walksAllowed: Int,
    val strikeouts: Int,
    val homeRunsAllowed: Int,
    val era: BigDecimal?,
    val whip: BigDecimal,
)
