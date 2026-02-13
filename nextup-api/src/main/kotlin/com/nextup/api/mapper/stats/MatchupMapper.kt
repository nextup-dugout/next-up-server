package com.nextup.api.mapper.stats

import com.nextup.api.dto.stats.MatchupHistoryResponse
import com.nextup.api.dto.stats.MatchupResponse
import com.nextup.api.dto.stats.MatchupStatsResponse
import com.nextup.core.service.stats.dto.MatchupDto
import com.nextup.core.service.stats.dto.MatchupHistoryDto
import com.nextup.core.service.stats.dto.MatchupStatsDto

fun MatchupDto.toResponse(): MatchupResponse =
    MatchupResponse(
        pitcherId = pitcherId,
        pitcherName = pitcherName,
        batterId = batterId,
        batterName = batterName,
        year = year,
        stats = stats.toResponse(),
        history = history.map { it.toResponse() },
    )

fun MatchupStatsDto.toResponse(): MatchupStatsResponse =
    MatchupStatsResponse(
        plateAppearances = plateAppearances,
        atBats = atBats,
        hits = hits,
        doubles = doubles,
        triples = triples,
        homeRuns = homeRuns,
        walks = walks,
        strikeouts = strikeouts,
        hitByPitch = hitByPitch,
        sacrificeFlies = sacrificeFlies,
        runsBattedIn = runsBattedIn,
        battingAverage = battingAverage,
        onBasePercentage = onBasePercentage,
        sluggingPercentage = sluggingPercentage,
    )

fun MatchupHistoryDto.toResponse(): MatchupHistoryResponse =
    MatchupHistoryResponse(
        gameId = gameId,
        gameDate = gameDate,
        result = result,
        description = description,
    )
