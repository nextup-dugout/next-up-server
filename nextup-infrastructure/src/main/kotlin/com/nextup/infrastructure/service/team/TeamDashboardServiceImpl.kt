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
import com.nextup.core.service.team.TeamDashboardService
import com.nextup.core.service.team.TeamMembershipService
import com.nextup.core.service.team.dto.GameSummaryForDashboardDto
import com.nextup.core.service.team.dto.PollSummaryDto
import com.nextup.core.service.team.dto.StandingEntryDto
import com.nextup.core.service.team.dto.TeamDashboardDto
import com.nextup.core.service.team.dto.TeamStatsSummaryDto
import com.nextup.core.service.team.dto.TeamSummaryDto
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
) : TeamDashboardService {

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

        // 해당 대회의 모든 GameTeam 조회
        val competitionGameTeams = gameTeamRepository.findAllByCompetitionId(primaryCompetitionId)
        val decidedGameTeams =
            gameTeamRepository.findAllByCompetitionIdWithDecidedResult(primaryCompetitionId)

        if (competitionGameTeams.isEmpty()) return null

        // 대회명 추출
        val competitionName =
            competitionGameTeams
                .firstOrNull()
                ?.game
                ?.competition
                ?.name ?: return null

        // 대회 참여 팀 ID 목록
        val allTeamIds = competitionGameTeams.map { it.team.id }.distinct()

        // 팀별 성적 계산
        val teamStatsMap = calculateTeamStatsForStanding(decidedGameTeams, competitionGameTeams)

        // 정렬 (승률 DESC, 득실차 DESC, 득점 DESC)
        val sortedStats =
            allTeamIds
                .mapNotNull { id -> teamStatsMap[id] }
                .sortedWith(
                    compareByDescending<TeamStandingStats> { it.winningPercentage }
                        .thenByDescending { it.runDifferential }
                        .thenByDescending { it.runsScored },
                )

        // 해당 팀의 순위 찾기
        val teamIndex = sortedStats.indexOfFirst { it.teamId == teamId }
        if (teamIndex < 0) return null

        val teamStats = sortedStats[teamIndex]
        val leader = sortedStats.firstOrNull()

        val gamesBehind =
            if (leader == null || leader.teamId == teamId) {
                BigDecimal.ZERO
            } else {
                val winDiff = leader.wins - teamStats.wins
                val lossDiff = teamStats.losses - leader.losses
                BigDecimal(winDiff + lossDiff)
                    .divide(BigDecimal(2), 1, RoundingMode.HALF_UP)
            }

        return StandingEntryDto(
            rank = teamIndex + 1,
            teamId = teamId,
            teamName = teamStats.teamName,
            competitionId = primaryCompetitionId,
            competitionName = competitionName,
            gamesPlayed = teamStats.gamesPlayed,
            wins = teamStats.wins,
            losses = teamStats.losses,
            draws = teamStats.draws,
            winningPercentage = teamStats.winningPercentage,
            gamesBehind = gamesBehind,
        )
    }

    /**
     * 순위 계산용 팀별 통계를 집계합니다.
     */
    private fun calculateTeamStatsForStanding(
        decidedGameTeams: List<com.nextup.core.domain.game.GameTeam>,
        allGameTeams: List<com.nextup.core.domain.game.GameTeam>,
    ): Map<Long, TeamStandingStats> {
        val decidedByTeam = decidedGameTeams.groupBy { it.team.id }
        val allTeamIds = allGameTeams.map { it.team.id }.distinct()

        return allTeamIds.associateWith { teamId ->
            val gameTeams = decidedByTeam[teamId] ?: emptyList()
            val teamName = allGameTeams.first { it.team.id == teamId }.team.name
            val wins = gameTeams.count { it.result == GameResult.WIN }
            val losses = gameTeams.count { it.result == GameResult.LOSS }
            val draws = gameTeams.count { it.result == GameResult.DRAW }
            val gamesPlayed = wins + losses + draws
            val runsScored = gameTeams.sumOf { it.totalScore }
            val runsAllowed =
                gameTeams.sumOf { gameTeam ->
                    decidedGameTeams
                        .filter { it.game.id == gameTeam.game.id && it.team.id != teamId }
                        .sumOf { it.totalScore }
                }
            val runDifferential = runsScored - runsAllowed
            val winningPercentage =
                if (gamesPlayed > 0) {
                    BigDecimal(wins)
                        .add(BigDecimal(draws).multiply(BigDecimal("0.5")))
                        .divide(BigDecimal(gamesPlayed), 3, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }

            TeamStandingStats(
                teamId = teamId,
                teamName = teamName,
                gamesPlayed = gamesPlayed,
                wins = wins,
                losses = losses,
                draws = draws,
                winningPercentage = winningPercentage,
                runsScored = runsScored,
                runDifferential = runDifferential,
            )
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

    /**
     * 순위 계산용 내부 데이터 클래스
     */
    private data class TeamStandingStats(
        val teamId: Long,
        val teamName: String,
        val gamesPlayed: Int,
        val wins: Int,
        val losses: Int,
        val draws: Int,
        val winningPercentage: BigDecimal,
        val runsScored: Int,
        val runDifferential: Int,
    )
}
