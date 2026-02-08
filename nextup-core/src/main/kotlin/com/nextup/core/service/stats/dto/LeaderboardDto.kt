package com.nextup.core.service.stats.dto

data class BattingLeaderDto(
    val rank: Int,
    val playerId: Long,
    val playerName: String,
    val teamName: String,
    val value: Double,
    val games: Int,
    val plateAppearances: Int,
)

data class PitchingLeaderDto(
    val rank: Int,
    val playerId: Long,
    val playerName: String,
    val teamName: String,
    val value: Double,
    val games: Int,
    val inningsPitched: Double,
)

enum class BattingCategory {
    BATTING_AVG,
    HOME_RUNS,
    RBI,
    HITS,
    STOLEN_BASES,
    OBP,
    SLG,
    OPS,
}

enum class PitchingCategory {
    WINS,
    SAVES,
    STRIKEOUTS,
    ERA,
    WHIP,
}
