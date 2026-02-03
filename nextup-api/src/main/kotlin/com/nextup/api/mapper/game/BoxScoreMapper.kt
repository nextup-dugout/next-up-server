package com.nextup.api.mapper.game

import com.nextup.api.dto.game.*
import com.nextup.core.service.game.dto.*

/**
 * BoxScoreDto를 BoxScoreResponse로 변환합니다.
 */
fun BoxScoreDto.toResponse(): BoxScoreResponse = BoxScoreResponse(
    gameId = gameId,
    homeTeam = homeTeam.toResponse(),
    awayTeam = awayTeam.toResponse(),
    currentInning = currentInning,
    gameStatus = gameStatus
)

/**
 * TeamBoxScoreDto를 TeamBoxScoreResponse로 변환합니다.
 */
fun TeamBoxScoreDto.toResponse(): TeamBoxScoreResponse = TeamBoxScoreResponse(
    teamId = teamId,
    teamName = teamName,
    inningScores = inningScores,
    runs = runs,
    hits = hits,
    errors = errors,
    batters = batters.map { it.toResponse() },
    pitchers = pitchers.map { it.toResponse() }
)

/**
 * BatterLineDto를 BatterLineResponse로 변환합니다.
 */
fun BatterLineDto.toResponse(): BatterLineResponse = BatterLineResponse(
    playerId = playerId,
    name = name,
    position = position,
    battingOrder = battingOrder,
    plateAppearances = plateAppearances,
    atBats = atBats,
    runs = runs,
    hits = hits,
    rbis = rbis,
    walks = walks,
    strikeouts = strikeouts,
    avg = avg
)

/**
 * PitcherLineDto를 PitcherLineResponse로 변환합니다.
 */
fun PitcherLineDto.toResponse(): PitcherLineResponse = PitcherLineResponse(
    playerId = playerId,
    name = name,
    inningsPitched = inningsPitched,
    hits = hits,
    runs = runs,
    earnedRuns = earnedRuns,
    walks = walks,
    strikeouts = strikeouts,
    homeRuns = homeRuns,
    decision = decision,
    era = era
)
