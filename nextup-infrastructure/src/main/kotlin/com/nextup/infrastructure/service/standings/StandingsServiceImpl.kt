package com.nextup.infrastructure.service.standings

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.service.standings.StandingsService
import com.nextup.core.service.standings.dto.StandingsDto
import com.nextup.core.service.standings.dto.TeamStandingDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * 순위표 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class StandingsServiceImpl(
    private val competitionRepository: CompetitionRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
) : StandingsService {
    override fun getStandings(competitionId: Long): StandingsDto {
        // 1. 대회 조회
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        // 2. 대회의 모든 GameTeam 조회
        val allGameTeams = gameTeamRepository.findAllByCompetitionId(competitionId)
        val decidedGameTeams = gameTeamRepository.findAllByCompetitionIdWithDecidedResult(competitionId)

        // 3. 팀별로 그룹화하여 통계 계산
        val teamStats = calculateTeamStats(decidedGameTeams, allGameTeams)

        // 4. 정렬: 승률 DESC, 득실차 DESC, 득점 DESC
        val sortedStats =
            teamStats.sortedWith(
                compareByDescending<TeamStats> { it.winningPercentage }
                    .thenByDescending { it.runDifferential }
                    .thenByDescending { it.runsScored },
            )

        // 5. 순위 부여 및 게임 차 계산
        val leader = sortedStats.firstOrNull()
        val standings =
            sortedStats.mapIndexed { index, stats ->
                TeamStandingDto(
                    rank = index + 1,
                    teamId = stats.teamId,
                    teamName = stats.teamName,
                    gamesPlayed = stats.gamesPlayed,
                    remainingGames = stats.remainingGames,
                    wins = stats.wins,
                    losses = stats.losses,
                    draws = stats.draws,
                    winningPercentage = stats.winningPercentage,
                    gamesBehind = calculateGamesBehind(leader, stats),
                    runsScored = stats.runsScored,
                    runsAllowed = stats.runsAllowed,
                    runDifferential = stats.runDifferential,
                )
            }

        return StandingsDto(
            competitionId = competition.id,
            competitionName = competition.name,
            totalGamesPerTeam = calculateTotalGamesPerTeam(allGameTeams),
            standings = standings,
            lastUpdated = LocalDateTime.now(),
        )
    }

    /**
     * 팀별 통계 계산
     */
    private fun calculateTeamStats(
        decidedGameTeams: List<GameTeam>,
        allGameTeams: List<GameTeam>,
    ): List<TeamStats> {
        // 팀별 그룹화 (결과가 확정된 경기만)
        val teamStatsMap =
            decidedGameTeams.groupBy { it.team.id }.mapValues { (teamId, gameTeams) ->
                val teamName = gameTeams.first().team.name
                val wins = gameTeams.count { it.result == GameResult.WIN }
                val losses = gameTeams.count { it.result == GameResult.LOSS }
                val draws = gameTeams.count { it.result == GameResult.DRAW }
                val gamesPlayed = wins + losses + draws
                val runsScored = gameTeams.sumOf { it.totalScore }
                val runsAllowed =
                    gameTeams.sumOf { gameTeam ->
                        // 상대팀 점수 계산
                        val game = gameTeam.game
                        val opponentGameTeams =
                            decidedGameTeams.filter {
                                it.game.id == game.id && it.team.id != teamId
                            }
                        opponentGameTeams.sumOf { it.totalScore }
                    }
                val runDifferential = runsScored - runsAllowed

                // 승률 계산 (무승부는 0.5승으로 계산)
                val winningPercentage =
                    if (gamesPlayed > 0) {
                        BigDecimal(wins)
                            .add(BigDecimal(draws).multiply(BigDecimal("0.5")))
                            .divide(BigDecimal(gamesPlayed), 3, RoundingMode.HALF_UP)
                    } else {
                        BigDecimal.ZERO
                    }

                // 남은 경기 계산
                val totalScheduledGames = allGameTeams.count { it.team.id == teamId }
                val remainingGames = totalScheduledGames - gamesPlayed

                TeamStats(
                    teamId = teamId,
                    teamName = teamName,
                    gamesPlayed = gamesPlayed,
                    remainingGames = remainingGames,
                    wins = wins,
                    losses = losses,
                    draws = draws,
                    winningPercentage = winningPercentage,
                    runsScored = runsScored,
                    runsAllowed = runsAllowed,
                    runDifferential = runDifferential,
                )
            }

        // 모든 팀 포함 (경기가 없는 팀도 포함)
        val allTeamIds = allGameTeams.map { it.team.id }.distinct()
        return allTeamIds.map { teamId ->
            teamStatsMap[teamId] ?: run {
                // 경기가 없는 팀
                val teamName = allGameTeams.first { it.team.id == teamId }.team.name
                val totalScheduledGames = allGameTeams.count { it.team.id == teamId }
                TeamStats(
                    teamId = teamId,
                    teamName = teamName,
                    gamesPlayed = 0,
                    remainingGames = totalScheduledGames,
                    wins = 0,
                    losses = 0,
                    draws = 0,
                    winningPercentage = BigDecimal.ZERO,
                    runsScored = 0,
                    runsAllowed = 0,
                    runDifferential = 0,
                )
            }
        }
    }

    /**
     * 선두팀과의 게임 차 계산
     */
    private fun calculateGamesBehind(
        leader: TeamStats?,
        team: TeamStats,
    ): BigDecimal {
        if (leader == null || leader.teamId == team.teamId) {
            return BigDecimal.ZERO
        }

        // 게임 차 = ((선두 승수 - 현재 승수) - (선두 패수 - 현재 패수)) / 2
        val winDiff = leader.wins - team.wins
        val lossDiff = team.losses - leader.losses
        return BigDecimal(winDiff + lossDiff)
            .divide(BigDecimal(2), 1, RoundingMode.HALF_UP)
    }

    /**
     * 팀당 총 경기 수 계산 (정규 시즌 기준)
     */
    private fun calculateTotalGamesPerTeam(allGameTeams: List<GameTeam>): Int {
        if (allGameTeams.isEmpty()) {
            return 0
        }

        // 팀별 예정된 총 경기 수의 최댓값 반환
        return allGameTeams.groupBy { it.team.id }
            .mapValues { (_, gameTeams) -> gameTeams.size }
            .values
            .maxOrNull() ?: 0
    }

    /**
     * 팀 통계 내부 데이터 클래스
     */
    private data class TeamStats(
        val teamId: Long,
        val teamName: String,
        val gamesPlayed: Int,
        val remainingGames: Int,
        val wins: Int,
        val losses: Int,
        val draws: Int,
        val winningPercentage: BigDecimal,
        val runsScored: Int,
        val runsAllowed: Int,
        val runDifferential: Int,
    )
}
