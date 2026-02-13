package com.nextup.api.dto.stats

import java.math.BigDecimal

/**
 * 최근 N경기 폼 분석 응답
 */
data class RecentFormResponse(
    val playerId: Long,
    val playerName: String,
    val type: FormTypeResponse,
    val gamesRequested: Int,
    val gamesFound: Int,
    val trend: FormTrendResponse,
    val trendDescription: String,
    val batting: RecentBattingFormResponse?,
    val pitching: RecentPitchingFormResponse?,
)

/**
 * 폼 분석 타입 응답
 */
enum class FormTypeResponse {
    BATTING,
    PITCHING,
}

/**
 * 폼 트렌드 응답
 */
enum class FormTrendResponse {
    UP,
    DOWN,
    STABLE,
}

/**
 * 최근 타격 폼 응답
 */
data class RecentBattingFormResponse(
    val games: List<GameBattingResponse>,
    val totalAtBats: Int,
    val totalHits: Int,
    val totalHomeRuns: Int,
    val totalRbis: Int,
    val totalRuns: Int,
    val recentAverage: BigDecimal,
    val overallAverage: BigDecimal?,
)

/**
 * 경기별 타격 기록 응답
 */
data class GameBattingResponse(
    val gameId: Long,
    val gameDate: String,
    val opponentName: String,
    val atBats: Int,
    val hits: Int,
    val homeRuns: Int,
    val rbis: Int,
    val runs: Int,
    val walks: Int,
    val strikeouts: Int,
)

/**
 * 최근 투수 폼 응답
 */
data class RecentPitchingFormResponse(
    val games: List<GamePitchingResponse>,
    val totalInningsPitchedOuts: Int,
    val inningsPitchedDisplay: String,
    val totalEarnedRuns: Int,
    val totalStrikeouts: Int,
    val recentEra: BigDecimal,
    val overallEra: BigDecimal?,
)

/**
 * 경기별 투수 기록 응답
 */
data class GamePitchingResponse(
    val gameId: Long,
    val gameDate: String,
    val opponentName: String,
    val inningsPitched: String,
    val earnedRuns: Int,
    val strikeouts: Int,
    val walksAllowed: Int,
    val hitsAllowed: Int,
    val decision: String?,
)
