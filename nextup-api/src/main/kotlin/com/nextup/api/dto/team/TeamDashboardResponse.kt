package com.nextup.api.dto.team

import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.service.team.dto.GameSummaryForDashboardDto
import com.nextup.core.service.team.dto.PollSummaryDto
import com.nextup.core.service.team.dto.StandingEntryDto
import com.nextup.core.service.team.dto.TeamDashboardDto
import com.nextup.core.service.team.dto.TeamStatsSummaryDto
import com.nextup.core.service.team.dto.TeamSummaryDto
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 팀 대시보드 응답 DTO
 */
data class TeamDashboardResponse(
    val team: DashboardTeamSummaryResponse,
    val memberCount: Int,
    val nextGame: GameSummaryForDashboardResponse?,
    val recentResults: List<GameSummaryForDashboardResponse>,
    val standing: StandingEntryResponse?,
    val activePoll: PollSummaryResponse?,
    val teamStats: TeamStatsSummaryResponse?,
) {
    companion object {
        fun from(dto: TeamDashboardDto): TeamDashboardResponse =
            TeamDashboardResponse(
                team = DashboardTeamSummaryResponse.from(dto.team),
                memberCount = dto.memberCount,
                nextGame = dto.nextGame?.let { GameSummaryForDashboardResponse.from(it) },
                recentResults = dto.recentResults.map { GameSummaryForDashboardResponse.from(it) },
                standing = dto.standing?.let { StandingEntryResponse.from(it) },
                activePoll = dto.activePoll?.let { PollSummaryResponse.from(it) },
                teamStats = dto.teamStats?.let { TeamStatsSummaryResponse.from(it) },
            )
    }
}

/**
 * 팀 요약 응답 DTO (대시보드 전용)
 */
data class DashboardTeamSummaryResponse(
    val teamId: Long,
    val name: String,
    val city: String,
    val abbreviation: String?,
    val leagueName: String?,
    val foundedYear: Int,
) {
    companion object {
        fun from(dto: TeamSummaryDto): DashboardTeamSummaryResponse =
            DashboardTeamSummaryResponse(
                teamId = dto.teamId,
                name = dto.name,
                city = dto.city,
                abbreviation = dto.abbreviation,
                leagueName = dto.leagueName,
                foundedYear = dto.foundedYear,
            )
    }
}

/**
 * 경기 요약 응답 DTO (대시보드 전용)
 */
data class GameSummaryForDashboardResponse(
    val gameId: Long,
    val competitionId: Long,
    val competitionName: String,
    val homeTeam: DashboardGameTeamResponse,
    val awayTeam: DashboardGameTeamResponse,
    val scheduledAt: LocalDateTime,
    val status: GameStatus,
    val statusDisplayName: String,
    val location: String?,
    val fieldName: String?,
) {
    companion object {
        fun from(dto: GameSummaryForDashboardDto): GameSummaryForDashboardResponse =
            GameSummaryForDashboardResponse(
                gameId = dto.gameId,
                competitionId = dto.competitionId,
                competitionName = dto.competitionName,
                homeTeam =
                    DashboardGameTeamResponse(
                        teamId = dto.homeTeamId,
                        teamName = dto.homeTeamName,
                        score = dto.homeScore,
                    ),
                awayTeam =
                    DashboardGameTeamResponse(
                        teamId = dto.awayTeamId,
                        teamName = dto.awayTeamName,
                        score = dto.awayScore,
                    ),
                scheduledAt = dto.scheduledAt,
                status = dto.status,
                statusDisplayName = dto.status.displayName,
                location = dto.location,
                fieldName = dto.fieldName,
            )
    }
}

/**
 * 대시보드용 게임 팀 응답 DTO
 */
data class DashboardGameTeamResponse(
    val teamId: Long,
    val teamName: String,
    val score: Int,
)

/**
 * 순위 항목 응답 DTO
 */
data class StandingEntryResponse(
    val rank: Int,
    val teamId: Long,
    val teamName: String,
    val competitionId: Long,
    val competitionName: String,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winningPercentage: BigDecimal,
    val gamesBehind: BigDecimal,
) {
    companion object {
        fun from(dto: StandingEntryDto): StandingEntryResponse =
            StandingEntryResponse(
                rank = dto.rank,
                teamId = dto.teamId,
                teamName = dto.teamName,
                competitionId = dto.competitionId,
                competitionName = dto.competitionName,
                gamesPlayed = dto.gamesPlayed,
                wins = dto.wins,
                losses = dto.losses,
                draws = dto.draws,
                winningPercentage = dto.winningPercentage,
                gamesBehind = dto.gamesBehind,
            )
    }
}

/**
 * 출석 투표 요약 응답 DTO
 */
data class PollSummaryResponse(
    val pollId: Long,
    val title: String,
    val eventDate: LocalDateTime,
    val deadline: LocalDateTime,
    val status: PollStatus,
) {
    companion object {
        fun from(dto: PollSummaryDto): PollSummaryResponse =
            PollSummaryResponse(
                pollId = dto.pollId,
                title = dto.title,
                eventDate = dto.eventDate,
                deadline = dto.deadline,
                status = dto.status,
            )
    }
}

/**
 * 팀 통계 요약 응답 DTO
 */
data class TeamStatsSummaryResponse(
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winningPercentage: BigDecimal,
    val teamBattingAverage: BigDecimal,
    val teamEra: BigDecimal,
) {
    companion object {
        fun from(dto: TeamStatsSummaryDto): TeamStatsSummaryResponse =
            TeamStatsSummaryResponse(
                gamesPlayed = dto.gamesPlayed,
                wins = dto.wins,
                losses = dto.losses,
                draws = dto.draws,
                winningPercentage = dto.winningPercentage,
                teamBattingAverage = dto.teamBattingAverage,
                teamEra = dto.teamEra,
            )
    }
}
