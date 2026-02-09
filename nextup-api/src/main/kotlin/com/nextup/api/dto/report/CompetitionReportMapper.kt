package com.nextup.api.dto.report

import com.nextup.api.dto.standings.TeamStandingResponse
import com.nextup.core.service.report.dto.CompetitionReportDto
import com.nextup.core.service.report.dto.CompetitionSummaryDto
import com.nextup.core.service.report.dto.HighScoringGameDto
import com.nextup.core.service.report.dto.WinStreakDto

/**
 * CompetitionReportDto → CompetitionReportResponse 변환
 */
fun CompetitionReportDto.toResponse(): CompetitionReportResponse =
    CompetitionReportResponse(
        competitionId = this.competitionId,
        competitionName = this.competitionName,
        season = this.season,
        standings = this.standings.map { TeamStandingResponse.from(it) },
        summary = this.summary.toResponse(),
    )

/**
 * CompetitionSummaryDto → CompetitionSummaryResponse 변환
 */
fun CompetitionSummaryDto.toResponse(): CompetitionSummaryResponse =
    CompetitionSummaryResponse(
        competitionId = this.competitionId,
        totalGames = this.totalGames,
        completedGames = this.completedGames,
        totalRuns = this.totalRuns,
        averageRunsPerGame = this.averageRunsPerGame,
        totalHits = this.totalHits,
        totalHomeRuns = this.totalHomeRuns,
        totalStrikeouts = this.totalStrikeouts,
        highestScoringGame = this.highestScoringGame?.toResponse(),
        longestWinStreak = this.longestWinStreak?.toResponse(),
    )

/**
 * HighScoringGameDto → HighScoringGameResponse 변환
 */
fun HighScoringGameDto.toResponse(): HighScoringGameResponse =
    HighScoringGameResponse(
        gameId = this.gameId,
        homeTeamName = this.homeTeamName,
        awayTeamName = this.awayTeamName,
        totalRuns = this.totalRuns,
        date = this.date,
    )

/**
 * WinStreakDto → WinStreakResponse 변환
 */
fun WinStreakDto.toResponse(): WinStreakResponse =
    WinStreakResponse(
        teamName = this.teamName,
        streakLength = this.streakLength,
    )
