package com.nextup.api.dto.stats

data class BattingLeaderResponse(
    val rank: Int,
    val playerId: Long,
    val playerName: String,
    val teamName: String,
    val value: Double,
    val games: Int,
    val plateAppearances: Int,
    val profileImageUrl: String? = null,
)

data class PitchingLeaderResponse(
    val rank: Int,
    val playerId: Long,
    val playerName: String,
    val teamName: String,
    val value: Double,
    val games: Int,
    val inningsPitched: Double,
    val profileImageUrl: String? = null,
)
