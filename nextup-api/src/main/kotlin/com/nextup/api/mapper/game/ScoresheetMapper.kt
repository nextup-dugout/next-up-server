package com.nextup.api.mapper.game

import com.nextup.api.dto.game.*
import com.nextup.core.service.game.dto.*

/**
 * ScoresheetDto -> ScoresheetResponse 변환
 */
fun ScoresheetDto.toResponse(): ScoresheetResponse =
    ScoresheetResponse(
        gameInfo = gameInfo.toResponse(),
        teams = teams.toResponse(),
        inningScores = inningScores.toResponse(),
        battingRecords = battingRecords.toResponse(),
        pitchingRecords = pitchingRecords.toResponse(),
        keyEvents = keyEvents.map { it.toResponse() },
    )

/**
 * GameInfoDto -> GameInfoResponse 변환
 */
fun GameInfoDto.toResponse(): GameInfoResponse =
    GameInfoResponse(
        gameId = gameId,
        competitionName = competitionName,
        gameNumber = gameNumber,
        scheduledAt = scheduledAt,
        startedAt = startedAt,
        endedAt = endedAt,
        location = location,
        fieldName = fieldName,
        status = status,
        currentInning = currentInning,
        totalInnings = totalInnings,
    )

/**
 * TeamsScoresheetDto -> TeamsResponse 변환
 */
fun TeamsScoresheetDto.toResponse(): TeamsResponse =
    TeamsResponse(
        home = home.toResponse(),
        away = away.toResponse(),
    )

/**
 * TeamScoresheetInfoDto -> TeamScoresheetResponse 변환
 */
fun TeamScoresheetInfoDto.toResponse(): TeamScoresheetResponse =
    TeamScoresheetResponse(
        teamId = teamId,
        teamName = teamName,
        logoUrl = logoUrl,
        totalScore = totalScore,
        totalHits = totalHits,
        totalErrors = totalErrors,
        result = result,
    )

/**
 * InningScoresDto -> InningScoresResponse 변환
 */
fun InningScoresDto.toResponse(): InningScoresResponse =
    InningScoresResponse(
        innings = innings,
        homeScores = homeScores,
        awayScores = awayScores,
    )

/**
 * BattingRecordsDto -> BattingRecordsResponse 변환
 */
fun BattingRecordsDto.toResponse(): BattingRecordsResponse =
    BattingRecordsResponse(
        home = home.map { it.toResponse() },
        away = away.map { it.toResponse() },
    )

/**
 * BatterScoresheetDto -> BatterScoresheetResponse 변환
 */
fun BatterScoresheetDto.toResponse(): BatterScoresheetResponse =
    BatterScoresheetResponse(
        playerId = playerId,
        name = name,
        backNumber = backNumber,
        position = position,
        battingOrder = battingOrder,
        plateAppearances = plateAppearances,
        atBats = atBats,
        runs = runs,
        hits = hits,
        doubles = doubles,
        triples = triples,
        homeRuns = homeRuns,
        rbis = rbis,
        walks = walks,
        strikeouts = strikeouts,
        stolenBases = stolenBases,
        avg = avg,
    )

/**
 * PitchingRecordsDto -> PitchingRecordsResponse 변환
 */
fun PitchingRecordsDto.toResponse(): PitchingRecordsResponse =
    PitchingRecordsResponse(
        home = home.map { it.toResponse() },
        away = away.map { it.toResponse() },
    )

/**
 * PitcherScoresheetDto -> PitcherScoresheetResponse 변환
 */
fun PitcherScoresheetDto.toResponse(): PitcherScoresheetResponse =
    PitcherScoresheetResponse(
        playerId = playerId,
        name = name,
        backNumber = backNumber,
        isStartingPitcher = isStartingPitcher,
        inningsPitched = inningsPitched,
        hitsAllowed = hitsAllowed,
        runsAllowed = runsAllowed,
        earnedRuns = earnedRuns,
        walks = walks,
        strikeouts = strikeouts,
        homeRunsAllowed = homeRunsAllowed,
        decision = decision,
        era = era,
    )

/**
 * KeyEventDto -> KeyEventResponse 변환
 */
fun KeyEventDto.toResponse(): KeyEventResponse =
    KeyEventResponse(
        inning = inning,
        description = description,
        timestamp = timestamp,
    )
