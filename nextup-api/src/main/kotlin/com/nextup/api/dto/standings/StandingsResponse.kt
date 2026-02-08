package com.nextup.api.dto.standings

import com.nextup.core.service.standings.dto.StandingsDto
import com.nextup.core.service.standings.dto.TeamStandingDto
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 순위표 응답 DTO (API - 조회 전용)
 *
 * 일반 사용자용 대회 순위표 정보
 */
data class StandingsResponse(
    val competitionId: Long,
    val competitionName: String,
    val totalGamesPerTeam: Int,
    val standings: List<TeamStandingResponse>,
    val lastUpdated: LocalDateTime,
    val playoffCutoff: Int?,
) {
    companion object {
        fun from(
            dto: StandingsDto,
            playoffCutoff: Int? = null,
        ): StandingsResponse =
            StandingsResponse(
                competitionId = dto.competitionId,
                competitionName = dto.competitionName,
                totalGamesPerTeam = dto.totalGamesPerTeam,
                standings =
                    dto.standings.map { standing ->
                        TeamStandingResponse.from(
                            dto = standing,
                            isPlayoffPosition = playoffCutoff != null && standing.rank <= playoffCutoff,
                        )
                    },
                lastUpdated = dto.lastUpdated,
                playoffCutoff = playoffCutoff,
            )
    }
}

/**
 * 팀 순위 정보 응답 DTO
 */
data class TeamStandingResponse(
    val rank: Int,
    val teamId: Long,
    val teamName: String,
    val gamesPlayed: Int,
    val remainingGames: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winningPercentage: BigDecimal,
    val gamesBehind: BigDecimal,
    val runsScored: Int,
    val runsAllowed: Int,
    val runDifferential: Int,
    val isPlayoffPosition: Boolean,
) {
    companion object {
        fun from(
            dto: TeamStandingDto,
            isPlayoffPosition: Boolean = false,
        ): TeamStandingResponse =
            TeamStandingResponse(
                rank = dto.rank,
                teamId = dto.teamId,
                teamName = dto.teamName,
                gamesPlayed = dto.gamesPlayed,
                remainingGames = dto.remainingGames,
                wins = dto.wins,
                losses = dto.losses,
                draws = dto.draws,
                winningPercentage = dto.winningPercentage,
                gamesBehind = dto.gamesBehind,
                runsScored = dto.runsScored,
                runsAllowed = dto.runsAllowed,
                runDifferential = dto.runDifferential,
                isPlayoffPosition = isPlayoffPosition,
            )
    }
}
