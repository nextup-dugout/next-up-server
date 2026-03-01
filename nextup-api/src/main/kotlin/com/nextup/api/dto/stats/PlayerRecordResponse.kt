package com.nextup.api.dto.stats

import com.nextup.core.service.stats.dto.RecordScope
import com.nextup.core.service.stats.dto.RecordType
import java.math.BigDecimal

/**
 * 선수 기록 응답 DTO
 */
data class PlayerRecordResponse(
    val playerId: Long,
    val playerName: String,
    val scope: RecordScope,
    val type: RecordType,
    val year: Int?,
    val competitionId: Long?,
    val competitionName: String?,
    val battingStats: BattingStatsResponse?,
    val pitchingStats: PitchingStatsResponse?,
)

/**
 * 타격 통계 응답 DTO
 */
data class BattingStatsResponse(
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
 * 투수 통계 응답 DTO
 */
data class PitchingStatsResponse(
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
