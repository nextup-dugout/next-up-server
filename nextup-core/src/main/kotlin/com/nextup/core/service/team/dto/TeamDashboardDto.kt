package com.nextup.core.service.team.dto

import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.game.GameStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 팀 대시보드 통합 DTO
 *
 * 팀 홈 화면에 필요한 모든 데이터를 집계하여 제공합니다.
 */
data class TeamDashboardDto(
    val team: TeamSummaryDto,
    val memberCount: Int,
    val nextGame: GameSummaryForDashboardDto?,
    val recentResults: List<GameSummaryForDashboardDto>,
    val standing: StandingEntryDto?,
    val activePoll: PollSummaryDto?,
    val teamStats: TeamStatsSummaryDto?,
)

/**
 * 팀 요약 DTO
 */
data class TeamSummaryDto(
    val teamId: Long,
    val name: String,
    val city: String,
    val abbreviation: String?,
    val leagueName: String?,
    val foundedYear: Int,
)

/**
 * 경기 요약 DTO (대시보드 전용)
 */
data class GameSummaryForDashboardDto(
    val gameId: Long,
    val competitionId: Long,
    val competitionName: String,
    val homeTeamId: Long,
    val homeTeamName: String,
    val awayTeamId: Long,
    val awayTeamName: String,
    val scheduledAt: LocalDateTime,
    val status: GameStatus,
    val homeScore: Int,
    val awayScore: Int,
    val location: String?,
    val fieldName: String?,
)

/**
 * 순위 항목 DTO
 */
data class StandingEntryDto(
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
)

/**
 * 출석 투표 요약 DTO
 */
data class PollSummaryDto(
    val pollId: Long,
    val title: String,
    val eventDate: LocalDateTime,
    val deadline: LocalDateTime,
    val status: PollStatus,
)

/**
 * 팀 통계 요약 DTO
 */
data class TeamStatsSummaryDto(
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winningPercentage: BigDecimal,
    val teamBattingAverage: BigDecimal,
    val teamEra: BigDecimal,
)
