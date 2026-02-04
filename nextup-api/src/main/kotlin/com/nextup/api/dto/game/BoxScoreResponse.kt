package com.nextup.api.dto.game

/**
 * 박스스코어 응답 DTO
 */
data class BoxScoreResponse(
    val gameId: Long,
    val homeTeam: TeamBoxScoreResponse,
    val awayTeam: TeamBoxScoreResponse,
    val currentInning: String,
    val gameStatus: String,
)

/**
 * 팀별 박스스코어 응답 DTO
 */
data class TeamBoxScoreResponse(
    val teamId: Long,
    val teamName: String,
    val inningScores: List<Int>,
    val runs: Int,
    val hits: Int,
    val errors: Int,
    val batters: List<BatterLineResponse>,
    val pitchers: List<PitcherLineResponse>,
)

/**
 * 타자 라인 응답 DTO
 */
data class BatterLineResponse(
    val playerId: Long,
    val name: String,
    val position: String,
    val battingOrder: Int?,
    val plateAppearances: Int,
    val atBats: Int,
    val runs: Int,
    val hits: Int,
    val rbis: Int,
    val walks: Int,
    val strikeouts: Int,
    val avg: String,
)

/**
 * 투수 라인 응답 DTO
 */
data class PitcherLineResponse(
    val playerId: Long,
    val name: String,
    val inningsPitched: String,
    val hits: Int,
    val runs: Int,
    val earnedRuns: Int,
    val walks: Int,
    val strikeouts: Int,
    val homeRuns: Int,
    val decision: String?,
    val era: String,
)
