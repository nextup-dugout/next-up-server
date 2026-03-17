package com.nextup.api.mapper.player

import com.nextup.api.dto.player.GameLogBattingResponse
import com.nextup.api.dto.player.GameLogFieldingResponse
import com.nextup.api.dto.player.GameLogPitchingResponse
import com.nextup.api.dto.player.GameLogResponse
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.service.game.GameLogEntry

/**
 * GameLogEntry를 GameLogResponse DTO로 변환하는 Extension Function
 */
fun GameLogEntry.toResponse(): GameLogResponse {
    val game = gamePlayer.gameTeam.game
    val myTeam = gamePlayer.gameTeam
    val opponentTeam = game.gameTeams.firstOrNull { it.id != myTeam.id }

    return GameLogResponse(
        gameId = game.id,
        scheduledAt = game.scheduledAt,
        opponentTeamName = opponentTeam?.team?.name,
        result = myTeam.result.displayName,
        position = gamePlayer.position.name,
        battingOrder = gamePlayer.battingOrder,
        batting =
            battingRecord?.let {
                GameLogBattingResponse(
                    atBats = it.atBats,
                    hits = it.hits,
                    runs = it.runs,
                    runsBattedIn = it.runsBattedIn,
                    homeRuns = it.homeRuns,
                    walks = it.walks,
                    strikeouts = it.strikeouts,
                    stolenBases = it.stolenBases,
                    battingAverage = it.battingAverage.toPlainString(),
                )
            },
        pitching =
            pitchingRecord?.let {
                GameLogPitchingResponse(
                    inningsPitched = it.inningsPitchedDisplay,
                    earnedRuns = it.earnedRuns,
                    strikeouts = it.strikeouts,
                    walks = it.walksAllowed,
                    hitsAllowed = it.hitsAllowed,
                    decision =
                        if (it.decision != PitchingDecision.NONE) it.decision.displayName else null,
                    era = it.earnedRunAverage?.toPlainString() ?: "-",
                )
            },
        fielding =
            fieldingRecord?.let {
                GameLogFieldingResponse(
                    putOuts = it.putOuts,
                    assists = it.assists,
                    errors = it.errors,
                    doublePlays = it.doublePlays,
                    fieldingPercentage =
                        it.fieldingPercentage?.toPlainString(),
                )
            },
    )
}

/**
 * List<GameLogEntry>를 List<GameLogResponse>로 변환
 */
fun List<GameLogEntry>.toResponse(): List<GameLogResponse> = this.map { it.toResponse() }
