package com.nextup.api.dto.player

import java.time.LocalDateTime

/**
 * 선수 게임 로그 응답 DTO
 *
 * 선수의 경기별 기록을 제공합니다.
 */
data class GameLogResponse(
    val gameId: Long,
    val scheduledAt: LocalDateTime,
    val opponentTeamName: String?,
    val result: String?,
    val position: String,
    val battingOrder: Int?,
    val batting: GameLogBattingResponse?,
    val pitching: GameLogPitchingResponse?,
    val fielding: GameLogFieldingResponse?,
)

/**
 * 게임 로그 타격 기록 요약
 */
data class GameLogBattingResponse(
    val atBats: Int,
    val hits: Int,
    val runs: Int,
    val runsBattedIn: Int,
    val homeRuns: Int,
    val walks: Int,
    val strikeouts: Int,
    val stolenBases: Int,
    val battingAverage: String,
)

/**
 * 게임 로그 투수 기록 요약
 */
data class GameLogPitchingResponse(
    val inningsPitched: String,
    val earnedRuns: Int,
    val strikeouts: Int,
    val walks: Int,
    val hitsAllowed: Int,
    val decision: String?,
    val era: String,
)

/**
 * 게임 로그 수비 기록 요약
 */
data class GameLogFieldingResponse(
    val putOuts: Int,
    val assists: Int,
    val errors: Int,
    val doublePlays: Int,
    val fieldingPercentage: String?,
)
