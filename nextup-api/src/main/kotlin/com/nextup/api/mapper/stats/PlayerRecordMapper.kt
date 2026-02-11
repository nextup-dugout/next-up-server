package com.nextup.api.mapper.stats

import com.nextup.api.dto.stats.BattingStatsResponse
import com.nextup.api.dto.stats.PitchingStatsResponse
import com.nextup.api.dto.stats.PlayerRecordResponse
import com.nextup.core.service.stats.dto.BattingStatsDto
import com.nextup.core.service.stats.dto.PitchingStatsDto
import com.nextup.core.service.stats.dto.PlayerRecordDto

/**
 * PlayerRecordDto를 PlayerRecordResponse로 변환합니다.
 */
fun PlayerRecordDto.toResponse(): PlayerRecordResponse =
    PlayerRecordResponse(
        playerId = playerId,
        playerName = playerName,
        scope = scope,
        type = type,
        year = year,
        competitionId = competitionId,
        competitionName = competitionName,
        battingStats = battingStats?.toResponse(),
        pitchingStats = pitchingStats?.toResponse(),
    )

/**
 * BattingStatsDto를 BattingStatsResponse로 변환합니다.
 */
fun BattingStatsDto.toResponse(): BattingStatsResponse =
    BattingStatsResponse(
        gamesPlayed = gamesPlayed,
        plateAppearances = plateAppearances,
        atBats = atBats,
        hits = hits,
        doubles = doubles,
        triples = triples,
        homeRuns = homeRuns,
        runs = runs,
        runsBattedIn = runsBattedIn,
        walks = walks,
        strikeouts = strikeouts,
        stolenBases = stolenBases,
        battingAverage = battingAverage,
        onBasePercentage = onBasePercentage,
        sluggingPercentage = sluggingPercentage,
        ops = ops,
    )

/**
 * PitchingStatsDto를 PitchingStatsResponse로 변환합니다.
 */
fun PitchingStatsDto.toResponse(): PitchingStatsResponse =
    PitchingStatsResponse(
        gamesPlayed = gamesPlayed,
        gamesStarted = gamesStarted,
        inningsPitched = inningsPitched,
        wins = wins,
        losses = losses,
        saves = saves,
        holds = holds,
        earnedRuns = earnedRuns,
        hitsAllowed = hitsAllowed,
        walksAllowed = walksAllowed,
        strikeouts = strikeouts,
        homeRunsAllowed = homeRunsAllowed,
        era = era,
        whip = whip,
    )
