package com.nextup.core.service.standings.dto

import java.math.BigDecimal

/**
 * 팀 순위 정보 DTO
 */
data class TeamStandingDto(
    val rank: Int,
    val teamId: Long,
    val teamName: String,
    val gamesPlayed: Int,
    val remainingGames: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winningPercentage: BigDecimal,
    val gamesBehind: BigDecimal,
    val runsScored: Int,
    val runsAllowed: Int,
    val runDifferential: Int,
    val logoUrl: String? = null,
)
