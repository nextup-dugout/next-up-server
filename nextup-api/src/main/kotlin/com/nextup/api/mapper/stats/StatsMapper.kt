package com.nextup.api.mapper.stats

import com.nextup.api.dto.stats.CareerBattingStatsResponse
import com.nextup.api.dto.stats.CareerPitchingStatsResponse
import com.nextup.api.dto.stats.SeasonBattingStatsResponse
import com.nextup.api.dto.stats.SeasonPitchingStatsResponse
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats

/**
 * SeasonBattingStats Entity를 SeasonBattingStatsResponse DTO로 변환하는 Extension Function
 */
fun SeasonBattingStats.toResponse(): SeasonBattingStatsResponse =
    SeasonBattingStatsResponse(
        // 메타 정보
        id = this.id,
        playerId = this.player.id,
        year = this.year,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        // 출전 정보
        gamesPlayed = this.gamesPlayed,
        // 기본 타격 기록
        plateAppearances = this.plateAppearances,
        atBats = this.atBats,
        hits = this.hits,
        doubles = this.doubles,
        triples = this.triples,
        homeRuns = this.homeRuns,
        runs = this.runs,
        runsBattedIn = this.runsBattedIn,
        walks = this.walks,
        intentionalWalks = this.intentionalWalks,
        hitByPitch = this.hitByPitch,
        strikeouts = this.strikeouts,
        sacrificeBunts = this.sacrificeBunts,
        sacrificeFlies = this.sacrificeFlies,
        stolenBases = this.stolenBases,
        caughtStealing = this.caughtStealing,
        groundedIntoDoublePlays = this.groundedIntoDoublePlays,
        // 계산 속성
        singles = this.singles,
        totalBases = this.totalBases,
        extraBaseHits = this.extraBaseHits,
        sacrifices = this.sacrifices,
        totalWalks = this.totalWalks,
        // BigDecimal → String 변환 (소수점 3자리 고정)
        battingAverage = this.battingAverage.toPlainString(),
        onBasePercentage = this.onBasePercentage.toPlainString(),
        sluggingPercentage = this.sluggingPercentage.toPlainString(),
        ops = this.ops.toPlainString(),
        stolenBasePercentage = this.stolenBasePercentage.toPlainString(),
    )

/**
 * SeasonPitchingStats Entity를 SeasonPitchingStatsResponse DTO로 변환하는 Extension Function
 */
fun SeasonPitchingStats.toResponse(): SeasonPitchingStatsResponse =
    SeasonPitchingStatsResponse(
        // 메타 정보
        id = this.id,
        playerId = this.player.id,
        year = this.year,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        // 출전 정보
        gamesPlayed = this.gamesPlayed,
        gamesStarted = this.gamesStarted,
        // 기본 투수 기록
        inningsPitchedOuts = this.inningsPitchedOuts,
        wins = this.wins,
        losses = this.losses,
        saves = this.saves,
        holds = this.holds,
        blownSaves = this.blownSaves,
        earnedRuns = this.earnedRuns,
        runsAllowed = this.runsAllowed,
        hitsAllowed = this.hitsAllowed,
        walksAllowed = this.walksAllowed,
        strikeouts = this.strikeouts,
        homeRunsAllowed = this.homeRunsAllowed,
        hitBatsmen = this.hitBatsmen,
        wildPitches = this.wildPitches,
        balks = this.balks,
        battersFaced = this.battersFaced,
        pitchesThrown = this.pitchesThrown,
        strikesThrown = this.strikesThrown,
        // 계산 속성
        completeInnings = this.completeInnings,
        remainingOuts = this.remainingOuts,
        inningsPitched = this.inningsPitched.toPlainString(),
        inningsPitchedDisplay = this.inningsPitchedDisplay,
        earnedRunAverage = this.earnedRunAverage.toPlainString(),
        whip = this.whip.toPlainString(),
        strikeoutsPer9 = this.strikeoutsPer9.toPlainString(),
        walksPer9 = this.walksPer9.toPlainString(),
        strikeoutToWalkRatio = this.strikeoutToWalkRatio.toPlainString(),
        strikePercentage = this.strikePercentage?.toPlainString(),
        unearnedRuns = this.unearnedRuns,
        winningPercentage = this.winningPercentage.toPlainString(),
    )

/**
 * CareerBattingStats Entity를 CareerBattingStatsResponse DTO로 변환하는 Extension Function
 */
fun CareerBattingStats.toResponse(): CareerBattingStatsResponse =
    CareerBattingStatsResponse(
        // 메타 정보
        id = this.id,
        playerId = this.player.id,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        // 시즌 및 출전 정보
        seasonsPlayed = this.seasonsPlayed,
        gamesPlayed = this.gamesPlayed,
        // 기본 타격 기록
        plateAppearances = this.plateAppearances,
        atBats = this.atBats,
        hits = this.hits,
        doubles = this.doubles,
        triples = this.triples,
        homeRuns = this.homeRuns,
        runs = this.runs,
        runsBattedIn = this.runsBattedIn,
        walks = this.walks,
        intentionalWalks = this.intentionalWalks,
        hitByPitch = this.hitByPitch,
        strikeouts = this.strikeouts,
        sacrificeBunts = this.sacrificeBunts,
        sacrificeFlies = this.sacrificeFlies,
        stolenBases = this.stolenBases,
        caughtStealing = this.caughtStealing,
        groundedIntoDoublePlays = this.groundedIntoDoublePlays,
        // 계산 속성
        singles = this.singles,
        totalBases = this.totalBases,
        extraBaseHits = this.extraBaseHits,
        sacrifices = this.sacrifices,
        totalWalks = this.totalWalks,
        // BigDecimal → String 변환 (소수점 3자리 고정)
        battingAverage = this.battingAverage.toPlainString(),
        onBasePercentage = this.onBasePercentage.toPlainString(),
        sluggingPercentage = this.sluggingPercentage.toPlainString(),
        ops = this.ops.toPlainString(),
        stolenBasePercentage = this.stolenBasePercentage.toPlainString(),
    )

/**
 * CareerPitchingStats Entity를 CareerPitchingStatsResponse DTO로 변환하는 Extension Function
 */
fun CareerPitchingStats.toResponse(): CareerPitchingStatsResponse =
    CareerPitchingStatsResponse(
        // 메타 정보
        id = this.id,
        playerId = this.player.id,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        // 시즌 및 출전 정보
        seasonsPlayed = this.seasonsPlayed,
        gamesPlayed = this.gamesPlayed,
        gamesStarted = this.gamesStarted,
        // 기본 투수 기록
        inningsPitchedOuts = this.inningsPitchedOuts,
        wins = this.wins,
        losses = this.losses,
        saves = this.saves,
        holds = this.holds,
        blownSaves = this.blownSaves,
        earnedRuns = this.earnedRuns,
        runsAllowed = this.runsAllowed,
        hitsAllowed = this.hitsAllowed,
        walksAllowed = this.walksAllowed,
        strikeouts = this.strikeouts,
        homeRunsAllowed = this.homeRunsAllowed,
        hitBatsmen = this.hitBatsmen,
        wildPitches = this.wildPitches,
        balks = this.balks,
        battersFaced = this.battersFaced,
        pitchesThrown = this.pitchesThrown,
        strikesThrown = this.strikesThrown,
        // 계산 속성
        completeInnings = this.completeInnings,
        remainingOuts = this.remainingOuts,
        inningsPitched = this.inningsPitched.toPlainString(),
        inningsPitchedDisplay = this.inningsPitchedDisplay,
        earnedRunAverage = this.earnedRunAverage.toPlainString(),
        whip = this.whip.toPlainString(),
        strikeoutsPer9 = this.strikeoutsPer9.toPlainString(),
        walksPer9 = this.walksPer9.toPlainString(),
        strikeoutToWalkRatio = this.strikeoutToWalkRatio.toPlainString(),
        strikePercentage = this.strikePercentage?.toPlainString(),
        unearnedRuns = this.unearnedRuns,
        winningPercentage = this.winningPercentage.toPlainString(),
    )

/**
 * List<SeasonBattingStats>를 List<SeasonBattingStatsResponse>로 변환
 */
fun List<SeasonBattingStats>.toSeasonBattingResponse(): List<SeasonBattingStatsResponse> = this.map { it.toResponse() }

/**
 * List<SeasonPitchingStats>를 List<SeasonPitchingStatsResponse>로 변환
 */
fun List<SeasonPitchingStats>.toSeasonPitchingResponse(): List<SeasonPitchingStatsResponse> =
    this.map {
        it.toResponse()
    }
