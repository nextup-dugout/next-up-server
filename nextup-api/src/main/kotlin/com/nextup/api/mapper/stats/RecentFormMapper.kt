package com.nextup.api.mapper.stats

import com.nextup.api.dto.stats.*
import com.nextup.core.service.stats.dto.*

/**
 * RecentFormDto → RecentFormResponse 변환
 */
fun RecentFormDto.toResponse(): RecentFormResponse =
    RecentFormResponse(
        playerId = playerId,
        playerName = playerName,
        type = type.toResponse(),
        gamesRequested = gamesRequested,
        gamesFound = gamesFound,
        trend = trend.toResponse(),
        trendDescription = trendDescription,
        batting = batting?.toResponse(),
        pitching = pitching?.toResponse(),
    )

/**
 * FormType → FormTypeResponse 변환
 */
fun FormType.toResponse(): FormTypeResponse =
    when (this) {
        FormType.BATTING -> FormTypeResponse.BATTING
        FormType.PITCHING -> FormTypeResponse.PITCHING
    }

/**
 * FormTrend → FormTrendResponse 변환
 */
fun FormTrend.toResponse(): FormTrendResponse =
    when (this) {
        FormTrend.UP -> FormTrendResponse.UP
        FormTrend.DOWN -> FormTrendResponse.DOWN
        FormTrend.STABLE -> FormTrendResponse.STABLE
    }

/**
 * RecentBattingFormDto → RecentBattingFormResponse 변환
 */
fun RecentBattingFormDto.toResponse(): RecentBattingFormResponse =
    RecentBattingFormResponse(
        games = games.map { it.toResponse() },
        totalAtBats = totalAtBats,
        totalHits = totalHits,
        totalHomeRuns = totalHomeRuns,
        totalRbis = totalRbis,
        totalRuns = totalRuns,
        recentAverage = recentAverage,
        overallAverage = overallAverage,
    )

/**
 * GameBattingDto → GameBattingResponse 변환
 */
fun GameBattingDto.toResponse(): GameBattingResponse =
    GameBattingResponse(
        gameId = gameId,
        gameDate = gameDate,
        opponentName = opponentName,
        atBats = atBats,
        hits = hits,
        homeRuns = homeRuns,
        rbis = rbis,
        runs = runs,
        walks = walks,
        strikeouts = strikeouts,
    )

/**
 * RecentPitchingFormDto → RecentPitchingFormResponse 변환
 */
fun RecentPitchingFormDto.toResponse(): RecentPitchingFormResponse =
    RecentPitchingFormResponse(
        games = games.map { it.toResponse() },
        totalInningsPitchedOuts = totalInningsPitchedOuts,
        inningsPitchedDisplay = inningsPitchedDisplay,
        totalEarnedRuns = totalEarnedRuns,
        totalStrikeouts = totalStrikeouts,
        recentEra = recentEra,
        overallEra = overallEra,
    )

/**
 * GamePitchingDto → GamePitchingResponse 변환
 */
fun GamePitchingDto.toResponse(): GamePitchingResponse =
    GamePitchingResponse(
        gameId = gameId,
        gameDate = gameDate,
        opponentName = opponentName,
        inningsPitched = inningsPitched,
        earnedRuns = earnedRuns,
        strikeouts = strikeouts,
        walksAllowed = walksAllowed,
        hitsAllowed = hitsAllowed,
        decision = decision,
    )
