package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.GameStatus
import java.time.LocalDateTime

/**
 * 경기 응답 DTO
 */
data class GameResponse(
    val id: Long,
    val competitionId: Long,
    val status: GameStatus,
    val currentInning: Int,
    val isTopInning: Boolean,
    val currentInningDisplay: String,
    val gameState: GameStateResponse,
    val scheduledAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?
)

/**
 * 경기 상태 응답 DTO
 */
data class GameStateResponse(
    val outs: Int,
    val balls: Int,
    val strikes: Int,
    val runnerOnFirstId: Long?,
    val runnerOnSecondId: Long?,
    val runnerOnThirdId: Long?,
    val homeBattingOrder: Int,
    val awayBattingOrder: Int,
    val currentPitcherId: Long?,
    val currentBatterId: Long?,
    val countDisplay: String
)
