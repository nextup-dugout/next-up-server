package com.nextup.api.dto.team

import com.nextup.core.service.team.dto.TeamMatchupDto
import com.nextup.core.service.team.dto.TeamMatchupGameDto
import java.time.LocalDate

/**
 * 팀 간 상대 전적 응답 DTO
 */
data class TeamMatchupResponse(
    val teamId: Long,
    val teamName: String,
    val opponentId: Long,
    val opponentName: String,
    val record: RecordSummary,
    val runs: RunsSummary,
)

/**
 * 전적 요약
 */
data class RecordSummary(
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val totalGames: Int,
    val winRate: Double,
)

/**
 * 득실점 요약
 */
data class RunsSummary(
    val runsScored: Int,
    val runsAllowed: Int,
    val avgRunsScored: Double,
    val avgRunsAllowed: Double,
    val runDifferential: Int,
)

/**
 * 팀 간 교전 경기 응답 DTO
 */
data class TeamMatchupGameResponse(
    val gameId: Long,
    val date: LocalDate,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val homeScore: Int,
    val awayScore: Int,
    val result: String,
    val venue: String?,
)

/**
 * TeamMatchupDto를 TeamMatchupResponse로 변환
 */
fun TeamMatchupDto.toResponse(): TeamMatchupResponse {
    val winRate =
        if (totalGames > 0) {
            wins.toDouble() / totalGames
        } else {
            0.0
        }

    return TeamMatchupResponse(
        teamId = teamId,
        teamName = teamName,
        opponentId = opponentId,
        opponentName = opponentName,
        record =
            RecordSummary(
                wins = wins,
                losses = losses,
                draws = draws,
                totalGames = totalGames,
                winRate = winRate,
            ),
        runs =
            RunsSummary(
                runsScored = runsScored,
                runsAllowed = runsAllowed,
                avgRunsScored = avgRunsScored,
                avgRunsAllowed = avgRunsAllowed,
                runDifferential = runsScored - runsAllowed,
            ),
    )
}

/**
 * TeamMatchupGameDto를 TeamMatchupGameResponse로 변환
 */
fun TeamMatchupGameDto.toResponse(): TeamMatchupGameResponse =
    TeamMatchupGameResponse(
        gameId = gameId,
        date = date,
        homeTeamId = homeTeamId,
        awayTeamId = awayTeamId,
        homeScore = homeScore,
        awayScore = awayScore,
        result = result,
        venue = venue,
    )

/**
 * List<TeamMatchupGameDto>를 List<TeamMatchupGameResponse>로 변환
 */
fun List<TeamMatchupGameDto>.toResponse(): List<TeamMatchupGameResponse> = map { it.toResponse() }
