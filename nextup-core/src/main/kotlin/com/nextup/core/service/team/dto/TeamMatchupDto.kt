package com.nextup.core.service.team.dto

import java.time.LocalDate

/**
 * 팀 간 상대 전적 DTO
 *
 * 두 팀의 대결 전적 통계를 담는 DTO
 */
data class TeamMatchupDto(
    val teamId: Long,
    val teamName: String,
    val opponentId: Long,
    val opponentName: String,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val totalGames: Int,
    val runsScored: Int,
    val runsAllowed: Int,
    val avgRunsScored: Double,
    val avgRunsAllowed: Double,
)

/**
 * 팀 간 교전 경기 기록 DTO
 *
 * 두 팀이 맞붙은 개별 경기 정보를 담는 DTO
 */
data class TeamMatchupGameDto(
    val gameId: Long,
    val date: LocalDate,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val homeScore: Int,
    val awayScore: Int,
    val result: String,
    val venue: String?,
)
