package com.nextup.api.dto.attendance

import com.nextup.core.service.attendance.PlayerParticipationRate
import java.math.BigDecimal

/**
 * 경기참여율 응답 DTO (자동 집계)
 */
data class GameParticipationRateResponse(
    val playerId: Long,
    val playerName: String,
    val gamesPlayed: Int,
    val totalTeamGames: Int,
    val participationRate: BigDecimal,
)

/**
 * Extension Functions for Mapping
 */
fun PlayerParticipationRate.toResponse(): GameParticipationRateResponse =
    GameParticipationRateResponse(
        playerId = this.playerId,
        playerName = this.playerName,
        gamesPlayed = this.gamesPlayed,
        totalTeamGames = this.totalTeamGames,
        participationRate = this.participationRate,
    )
