package com.nextup.core.service.stats.dto

import java.math.BigDecimal

/**
 * 최근 N경기 폼 분석 결과
 */
data class RecentFormDto(
    val playerId: Long,
    val playerName: String,
    val type: FormType,
    val gamesRequested: Int,
    val gamesFound: Int,
    val trend: FormTrend,
    val trendDescription: String,
    val batting: RecentBattingFormDto?,
    val pitching: RecentPitchingFormDto?,
)

/**
 * 폼 분석 타입
 */
enum class FormType {
    BATTING,
    PITCHING,
}

/**
 * 폼 트렌드
 */
enum class FormTrend {
    UP, // 상승세
    DOWN, // 하락세
    STABLE, // 안정적
}

/**
 * 최근 타격 폼
 */
data class RecentBattingFormDto(
    val games: List<GameBattingDto>,
    val totalAtBats: Int,
    val totalHits: Int,
    val totalHomeRuns: Int,
    val totalRbis: Int,
    val totalRuns: Int,
    val recentAverage: BigDecimal, // 최근 N경기 타율
    val overallAverage: BigDecimal?, // 시즌/통산 타율 (비교용)
)

/**
 * 경기별 타격 기록
 */
data class GameBattingDto(
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
 * 최근 투수 폼
 */
data class RecentPitchingFormDto(
    val games: List<GamePitchingDto>,
    val totalInningsPitchedOuts: Int,
    val inningsPitchedDisplay: String,
    val totalEarnedRuns: Int,
    val totalStrikeouts: Int,
    val recentEra: BigDecimal, // 최근 N경기 ERA
    val overallEra: BigDecimal?, // 시즌/통산 ERA (비교용)
)

/**
 * 경기별 투수 기록
 */
data class GamePitchingDto(
    val gameId: Long,
    val gameDate: String,
    val opponentName: String,
    val inningsPitched: String,
    val earnedRuns: Int,
    val strikeouts: Int,
    val walksAllowed: Int,
    val hitsAllowed: Int,
    val decision: String?, // 승/패/세/홀 등
)
