package com.nextup.infrastructure.service.team

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.standings.StandingsService
import com.nextup.core.service.team.TeamDashboardService
import com.nextup.core.service.team.TeamMembershipService
import com.nextup.core.service.team.dto.GameSummaryForDashboardDto
import com.nextup.core.service.team.dto.PollSummaryDto
import com.nextup.core.service.team.dto.StandingEntryDto
import com.nextup.core.service.team.dto.TeamDashboardDto
import com.nextup.core.service.team.dto.TeamStatsSummaryDto
import com.nextup.core.service.team.dto.TeamSummaryDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * 팀 대시보드 통합 서비스 구현
 *
 * 기존 서비스/레포지토리를 조합하여 팀 홈 화면에 필요한 데이터를 한 번에 제공합니다.
 * N+1 방지를 위해 배치 조회를 최대한 활용합니다.
 */
@Service
@Transactional(readOnly = true)
class TeamDashboardServiceImpl(
    private val teamRepository: TeamRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val teamMembershipService: TeamMembershipService,
    private val attendancePollRepository: AttendancePollRepositoryPort,
    private val standingsService: StandingsService,
) : TeamDashboardService {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RECENT_RESULTS_LIMIT = 5
        private const val NEXT_GAME_LIMIT = 1
    }

    override fun getTeamDashboard(teamId: Long): TeamDashboardDto {
        // 팀 조회 (리그 포함)
        val team =
            teamRepository.findByIdWithLeague(teamId)
                ?: throw TeamNotFoundException(teamId)

        // 멤버 수 조회
        val memberCount = teamMembershipService.getTeamMemberCount(teamId)

        // 팀이 참여한 모든 GameTeam 배치 조회 (N+1 방지)
        val allGameTeams = gameTeamRepository.findAllByTeamId(teamId)

        // 게임 ID 목록 추출
        val gameIds = allGameTeams.map { it.game.id }.distinct()

        // 게임 목록 배치 조회
        val games =
            if (gameIds.isNotEmpty()) {
                gameRepository.findAllByIds(gameIds)
            } else {
                emptyList()
            }

        // 게임 ID → GameTeam 맵 (배치 조회)
        val allGameTeamsByGameId =
            if (gameIds.isNotEmpty()) {
                gameTeamRepository.findAllByGameIds(gameIds).groupBy { it.game.id }
            } else {
                emptyMap()
            }

        val now = LocalDateTime.now()

        // 다음 경기 조회 (예정된 경기 중 가장 가까운 1개)
        val nextGame =
            games
                .filter { it.scheduledAt.isAfter(now) && it.status == GameStatus.SCHEDULED }
                .minByOrNull { it.scheduledAt }
                ?.let { game ->
                    val gameTeams = allGameTeamsByGameId[game.id] ?: emptyList()
                    toGameSummaryForDashboard(game, gameTeams)
                }

        // 최근 경기 결과 조회 (완료된 경기 최근 5개, 내림차순)
        val recentResults =
            games
                .filter { it.status.isCompleted() || it.status == GameStatus.CANCELLED }
                .sortedByDescending { it.scheduledAt }
                .take(RECENT_RESULTS_LIMIT)
                .map { game ->
                    val gameTeams = allGameTeamsByGameId[game.id] ?: emptyList()
                    toGameSummaryForDashboard(game, gameTeams)
                }

        // 순위 조회 (팀이 참여 중인 대회에서 해당 팀의 순위)
        val standing = resolveStanding(teamId, allGameTeams)

        // 진행 중인 출석 투표 조회
        val activePoll = resolveActivePoll(teamId)

        // 팀 통계 요약 (전체 시즌 기준)
        val teamStats = resolveTeamStats(allGameTeams)

        return TeamDashboardDto(
            team =
                TeamSummaryDto(
                    teamId = team.id,
                    name = team.name,
                    city = team.city,
                    abbreviation = team.abbreviation,
                    leagueName = team.league.name,
                    foundedYear = team.foundedYear,
                ),
            memberCount = memberCount,
            nextGame = nextGame,
            recentResults = recentResults,
            standing = standing,
            activePoll = activePoll,
            teamStats = teamStats,
        )
    }

    /**
     * 게임과 GameTeam 목록으로부터 대시보드용 경기 요약 DTO를 생성합니다.
     */
    private fun toGameSummaryForDashboard(
        game: com.nextup.core.domain.game.Game,
        gameTeams: List<com.nextup.core.domain.game.GameTeam>,
    ): GameSummaryForDashboardDto {
        val homeTeam = gameTeams.firstOrNull { it.homeAway == HomeAway.HOME }
        val awayTeam = gameTeams.firstOrNull { it.homeAway == HomeAway.AWAY }

        return GameSummaryForDashboardDto(
            gameId = game.id,
            competitionId = game.competition.id,
            competitionName = game.competition.name,
            homeTeamId = homeTeam?.team?.id ?: 0L,
            homeTeamName = homeTeam?.team?.name ?: "",
            awayTeamId = awayTeam?.team?.id ?: 0L,
            awayTeamName = awayTeam?.team?.name ?: "",
            scheduledAt = game.scheduledAt,
            status = game.status,
            homeScore = homeTeam?.totalScore ?: 0,
            awayScore = awayTeam?.totalScore ?: 0,
            location = game.location,
            fieldName = game.fieldName,
        )
    }

    /**
     * 팀이 참여 중인 가장 최근 대회에서의 순위 정보를 조회합니다.
     *
     * StandingsService에 위임하여 순위 계산 로직 중복을 제거합니다.
     * 팀이 여러 대회에 참여 중인 경우 가장 많은 경기를 치른 대회를 기준으로 합니다.
     */
    private fun resolveStanding(
        teamId: Long,
        allGameTeams: List<com.nextup.core.domain.game.GameTeam>,
    ): StandingEntryDto? {
        if (allGameTeams.isEmpty()) return null

        // 대회별 경기 수를 집계하여 가장 많은 경기를 치른 대회 선택
        val competitionGameCounts =
            allGameTeams
                .groupBy { it.game.competition.id }
                .mapValues { it.value.size }

        val primaryCompetitionId =
            competitionGameCounts.maxByOrNull { it.value }?.key ?: return null

        // StandingsService에 위임하여 순위 조회
        return try {
            val standings = standingsService.getStandings(primaryCompetitionId)
            val teamStanding =
                standings.standings.firstOrNull { it.teamId == teamId }
                    ?: return null

            StandingEntryDto(
                rank = teamStanding.rank,
                teamId = teamStanding.teamId,
                teamName = teamStanding.teamName,
                competitionId = standings.competitionId,
                competitionName = standings.competitionName,
                gamesPlayed = teamStanding.gamesPlayed,
                wins = teamStanding.wins,
                losses = teamStanding.losses,
                draws = teamStanding.draws,
                winningPercentage = teamStanding.winningPercentage,
                gamesBehind = teamStanding.gamesBehind,
            )
        } catch (e: Exception) {
            log.warn("순위 조회 실패 (competitionId={}): {}", primaryCompetitionId, e.message)
            null
        }
    }

    /**
     * 팀의 진행 중인(OPEN) 출석 투표 중 가장 최근 것을 조회합니다.
     */
    private fun resolveActivePoll(teamId: Long): PollSummaryDto? {
        val openPolls = attendancePollRepository.findByTeamId(teamId, PollStatus.OPEN)
        val poll = openPolls.minByOrNull { it.deadline } ?: return null

        return PollSummaryDto(
            pollId = poll.id,
            title = poll.title,
            eventDate = poll.eventDate,
            deadline = poll.deadline,
            status = poll.status,
        )
    }

    /**
     * 팀 통계 요약을 계산합니다.
     *
     * GameTeam의 결과 데이터로 기본 성적만 계산합니다.
     */
    private fun resolveTeamStats(allGameTeams: List<com.nextup.core.domain.game.GameTeam>,): TeamStatsSummaryDto? {
        if (allGameTeams.isEmpty()) return null

        val decidedGames =
            allGameTeams.filter {
                it.result == GameResult.WIN ||
                    it.result == GameResult.LOSS ||
                    it.result == GameResult.DRAW
            }
        val wins = decidedGames.count { it.result == GameResult.WIN }
        val losses = decidedGames.count { it.result == GameResult.LOSS }
        val draws = decidedGames.count { it.result == GameResult.DRAW }
        val gamesPlayed = wins + losses + draws

        val winningPercentage =
            if (wins + losses == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(wins)
                    .divide(BigDecimal(wins + losses), 3, RoundingMode.HALF_UP)
            }

        return TeamStatsSummaryDto(
            gamesPlayed = gamesPlayed,
            wins = wins,
            losses = losses,
            draws = draws,
            winningPercentage = winningPercentage,
            teamBattingAverage = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
            teamEra = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
        )
    }
}
