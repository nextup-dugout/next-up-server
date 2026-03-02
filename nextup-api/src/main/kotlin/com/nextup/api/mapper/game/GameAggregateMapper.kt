package com.nextup.api.mapper.game

import com.nextup.api.dto.game.GameAggregateInfoResponse
import com.nextup.api.dto.game.GameAggregateResponse
import com.nextup.api.dto.game.GameTeamSummary
import com.nextup.core.service.game.dto.GameAggregateDto
import com.nextup.core.service.game.dto.GameDetailDto

/**
 * GameAggregateDto -> GameAggregateResponse 변환
 */
fun GameAggregateDto.toResponse(): GameAggregateResponse =
    GameAggregateResponse(
        gameInfo = gameDetail.toAggregateInfoResponse(),
        boxScore = boxScore?.toResponse(),
        timeline = timeline.toResponse(),
        scoresheet = scoresheet?.toResponse(),
    )

/**
 * GameDetailDto -> GameAggregateInfoResponse 변환
 */
fun GameDetailDto.toAggregateInfoResponse(): GameAggregateInfoResponse =
    GameAggregateInfoResponse(
        gameId = gameId,
        competitionId = competitionId,
        competitionName = competitionName,
        homeTeam =
            GameTeamSummary(
                teamId = homeTeamId,
                teamName = homeTeamName,
                score = homeScore,
            ),
        awayTeam =
            GameTeamSummary(
                teamId = awayTeamId,
                teamName = awayTeamName,
                score = awayScore,
            ),
        scheduledAt = scheduledAt,
        status = status,
        statusDisplayName = status.displayName,
        location = location,
        fieldName = fieldName,
        gameNumber = gameNumber,
        currentInning = currentInning,
        totalInnings = totalInnings,
        startedAt = startedAt,
        endedAt = endedAt,
        note = note,
        forfeitReason = forfeitReason,
    )
