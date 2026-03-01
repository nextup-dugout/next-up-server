package com.nextup.core.service.game.dto

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 박스스코어 전체 DTO
 */
data class BoxScoreDto(
    val gameId: Long,
    val homeTeam: TeamBoxScoreDto,
    val awayTeam: TeamBoxScoreDto,
    val currentInning: String,
    val gameStatus: String,
)

/**
 * 팀별 박스스코어 DTO
 */
data class TeamBoxScoreDto(
    val teamId: Long,
    val teamName: String,
    val inningScores: List<Int>,
    val runs: Int,
    val hits: Int,
    val errors: Int,
    val batters: List<BatterLineDto>,
    val pitchers: List<PitcherLineDto>,
)

/**
 * 타자 라인 DTO
 */
data class BatterLineDto(
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
) {
    companion object {
        fun formatAverage(average: BigDecimal): String = average.setScale(3, RoundingMode.HALF_UP).toString()
    }
}

/**
 * 투수 라인 DTO
 */
data class PitcherLineDto(
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
) {
    companion object {
        fun formatERA(era: BigDecimal?): String = era?.setScale(2, RoundingMode.HALF_UP)?.toString() ?: "∞"
    }
}
