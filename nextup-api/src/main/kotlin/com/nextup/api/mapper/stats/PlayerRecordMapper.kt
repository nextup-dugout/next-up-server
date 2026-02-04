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
        playerId = this.playerId,
        playerName = this.playerName,
        scope = this.scope,
        type = this.type,
        year = this.year,
        competitionId = this.competitionId,
        competitionName = this.competitionName,
        battingStats = this.battingStats?.toResponse(),
        pitchingStats = this.pitchingStats?.toResponse(),
    )

/**
 * BattingStatsDto를 BattingStatsResponse로 변환합니다.
 */
fun BattingStatsDto.toResponse(): BattingStatsResponse =
    BattingStatsResponse(
        gamesPlayed = this.gamesPlayed,
        plateAppearances = this.plateAppearances,
        atBats = this.atBats,
        hits = this.hits,
        doubles = this.doubles,
        triples = this.triples,
        homeRuns = this.homeRuns,
        runs = this.runs,
        runsBattedIn = this.runsBattedIn,
        walks = this.walks,
        strikeouts = this.strikeouts,
        stolenBases = this.stolenBases,
        battingAverage = this.battingAverage,
        onBasePercentage = this.onBasePercentage,
        sluggingPercentage = this.sluggingPercentage,
        ops = this.ops,
    )

/**
 * PitchingStatsDto를 PitchingStatsResponse로 변환합니다.
 */
fun PitchingStatsDto.toResponse(): PitchingStatsResponse =
    PitchingStatsResponse(
        gamesPlayed = this.gamesPlayed,
        gamesStarted = this.gamesStarted,
        inningsPitched = this.inningsPitched,
        wins = this.wins,
        losses = this.losses,
        saves = this.saves,
        holds = this.holds,
        earnedRuns = this.earnedRuns,
        hitsAllowed = this.hitsAllowed,
        walksAllowed = this.walksAllowed,
        strikeouts = this.strikeouts,
        homeRunsAllowed = this.homeRunsAllowed,
        era = this.era,
        whip = this.whip,
    )
