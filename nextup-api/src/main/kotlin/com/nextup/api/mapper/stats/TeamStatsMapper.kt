package com.nextup.api.mapper.stats

import com.nextup.api.dto.stats.TeamBattingStatsResponse
import com.nextup.api.dto.stats.TeamPitchingStatsResponse
import com.nextup.api.dto.stats.TeamRecordResponse
import com.nextup.api.dto.stats.TeamStatsResponse
import com.nextup.core.service.stats.dto.TeamBattingStatsDto
import com.nextup.core.service.stats.dto.TeamPitchingStatsDto
import com.nextup.core.service.stats.dto.TeamRecordDto
import com.nextup.core.service.stats.dto.TeamStatsDto

fun TeamStatsDto.toResponse(): TeamStatsResponse =
    TeamStatsResponse(
        teamId = teamId,
        teamName = teamName,
        year = year,
        competitionId = competitionId,
        competitionName = competitionName,
        record = record.toResponse(),
        batting = batting.toResponse(),
        pitching = pitching.toResponse(),
    )

fun TeamRecordDto.toResponse(): TeamRecordResponse =
    TeamRecordResponse(
        gamesPlayed = gamesPlayed,
        wins = wins,
        losses = losses,
        draws = draws,
        winningPercentage = winningPercentage,
    )

fun TeamBattingStatsDto.toResponse(): TeamBattingStatsResponse =
    TeamBattingStatsResponse(
        totalAtBats = totalAtBats,
        totalHits = totalHits,
        totalHomeRuns = totalHomeRuns,
        totalRunsBattedIn = totalRunsBattedIn,
        totalRuns = totalRuns,
        teamBattingAverage = teamBattingAverage,
        teamOnBasePercentage = teamOnBasePercentage,
        teamSluggingPercentage = teamSluggingPercentage,
    )

fun TeamPitchingStatsDto.toResponse(): TeamPitchingStatsResponse =
    TeamPitchingStatsResponse(
        totalInningsPitchedOuts = totalInningsPitchedOuts,
        inningsPitchedDisplay = inningsPitchedDisplay,
        totalEarnedRuns = totalEarnedRuns,
        totalStrikeouts = totalStrikeouts,
        totalWalksAllowed = totalWalksAllowed,
        teamEra = teamEra,
        teamWhip = teamWhip,
    )
