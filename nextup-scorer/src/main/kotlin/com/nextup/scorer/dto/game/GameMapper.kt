package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameState

/**
 * Game Entity를 GameResponse DTO로 변환합니다.
 */
fun Game.toResponse(): GameResponse {
    return GameResponse(
        id = this.id,
        competitionId = this.competition.id,
        status = this.status,
        currentInning = this.currentInning,
        isTopInning = this.isTopInning,
        currentInningDisplay = this.currentInningDisplay,
        gameState = this.gameState.toResponse(),
        scheduledAt = this.scheduledAt,
        startedAt = this.startedAt,
        endedAt = this.endedAt
    )
}

/**
 * GameState를 GameStateResponse DTO로 변환합니다.
 */
fun GameState.toResponse(): GameStateResponse {
    return GameStateResponse(
        outs = this.outs,
        balls = this.balls,
        strikes = this.strikes,
        runnerOnFirstId = this.runnerOnFirstId,
        runnerOnSecondId = this.runnerOnSecondId,
        runnerOnThirdId = this.runnerOnThirdId,
        homeBattingOrder = this.homeBattingOrder,
        awayBattingOrder = this.awayBattingOrder,
        currentPitcherId = this.currentPitcherId,
        currentBatterId = this.currentBatterId,
        countDisplay = this.countDisplay
    )
}
