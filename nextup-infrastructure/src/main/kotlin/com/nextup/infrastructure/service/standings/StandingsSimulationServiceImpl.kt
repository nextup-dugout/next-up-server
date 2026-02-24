package com.nextup.infrastructure.service.standings

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidInputException
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.service.standings.StandingsSimulationService
import com.nextup.core.service.standings.dto.MagicNumber
import com.nextup.core.service.standings.dto.PlayoffScenarioResult
import com.nextup.core.service.standings.dto.RankChange
import com.nextup.core.service.standings.dto.SimulatedGameResult
import com.nextup.core.service.standings.dto.SimulationRequest
import com.nextup.core.service.standings.dto.SimulationResult
import com.nextup.core.service.standings.dto.TeamStandingDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

/**
 * 순위 시뮬레이션 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
class StandingsSimulationServiceImpl(
    private val competitionRepository: CompetitionRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
) : StandingsSimulationService {
    companion object {
        private const val MONTE_CARLO_ITERATIONS = 1000
        private const val MONTE_CARLO_THRESHOLD = 15
    }

    override fun calculateMagicNumbers(competitionId: Long): List<MagicNumber> {
        competitionRepository.findByIdOrNull(competitionId)
            ?: throw CompetitionNotFoundException(competitionId)

        val allGameTeams = gameTeamRepository.findAllByCompetitionId(competitionId)
        val decidedGameTeams =
            gameTeamRepository.findAllByCompetitionIdWithDecidedResult(competitionId)

        val teamStats = buildTeamStatsMap(decidedGameTeams, allGameTeams)
        val sortedStats =
            teamStats.values.sortedWith(
                compareByDescending<TeamStats> { it.winningPercentage }
                    .thenByDescending { it.runDifferential }
                    .thenByDescending { it.runsScored },
            )

        if (sortedStats.isEmpty()) return emptyList()

        val leader = sortedStats.first()

        return sortedStats.mapIndexed { index, stats ->
            val targetRank = index + 1
            // Magic Number = 리더의 남은 경기 + 1 - (리더 승수 - 해당팀 승수)
            val rawMagicNumber =
                leader.remainingGames + 1 - (leader.wins - stats.wins)
            // 수학적 탈락: 해당 팀이 남은 경기를 모두 이겨도 리더를 따라잡을 수 없는 경우
            val maxPossibleWins = stats.wins + stats.remainingGames
            val isEliminated = maxPossibleWins < leader.wins
            // 순위 확정: 선두팀의 경우 다른 팀이 따라잡을 수 없는 경우 (리더 입장에서)
            // 비선두팀의 경우 rawMagicNumber <= 0이면 수학적으로 상위 순위 진입 불가 → 탈락
            val isClinched =
                if (stats.teamId == leader.teamId) {
                    // 선두는 남은 경기 없고 적어도 1경기 이상 뛴 경우 확정
                    stats.remainingGames == 0 && stats.gamesPlayed > 0
                } else {
                    // 비선두팀: rawMagicNumber <= 0이면 현재 순위가 확정됨
                    // (더 이상 상위 팀을 따라잡을 수 없음)
                    rawMagicNumber <= 0 && !isEliminated
                }

            MagicNumber(
                teamId = stats.teamId,
                targetRank = targetRank,
                magicNumber = maxOf(0, rawMagicNumber),
                isClinched = isClinched,
                isEliminated = isEliminated,
            )
        }
    }

    override fun simulateStandings(
        competitionId: Long,
        request: SimulationRequest,
    ): SimulationResult {
        competitionRepository.findByIdOrNull(competitionId)
            ?: throw CompetitionNotFoundException(competitionId)

        validateSimulationRequest(request)

        val allGameTeams = gameTeamRepository.findAllByCompetitionId(competitionId)
        val decidedGameTeams =
            gameTeamRepository.findAllByCompetitionIdWithDecidedResult(competitionId)

        // 현재 실제 순위 계산
        val currentStats = buildTeamStatsMap(decidedGameTeams, allGameTeams)
        val currentStandings =
            buildStandings(currentStats.values.toList())

        // 가상 결과 적용 후 순위 계산
        val simulatedStats =
            applySimulatedResults(
                currentStats = currentStats.toMutableMap(),
                simulatedResults = request.gameResults,
                allGameTeams = allGameTeams,
            )
        val simulatedStandings = buildStandings(simulatedStats.values.toList())

        // 순위 변동 계산
        val changes = calculateRankChanges(currentStandings, simulatedStandings)

        return SimulationResult(
            standings = simulatedStandings,
            changes = changes,
        )
    }

    override fun calculatePlayoffScenarios(
        competitionId: Long,
        teamId: Long,
        playoffTeams: Int,
    ): PlayoffScenarioResult {
        competitionRepository.findByIdOrNull(competitionId)
            ?: throw CompetitionNotFoundException(competitionId)

        if (playoffTeams <= 0) {
            throw InvalidInputException(
                "INVALID_PLAYOFF_TEAMS",
                "플레이오프 진출 팀 수는 1 이상이어야 합니다.",
            )
        }

        val allGameTeams = gameTeamRepository.findAllByCompetitionId(competitionId)
        val decidedGameTeams =
            gameTeamRepository.findAllByCompetitionIdWithDecidedResult(competitionId)

        val currentStats = buildTeamStatsMap(decidedGameTeams, allGameTeams)
        val targetTeam =
            currentStats[teamId]
                ?: throw InvalidInputException(
                    "TEAM_NOT_IN_COMPETITION",
                    "해당 팀은 이 대회에 참여하지 않습니다. teamId=$teamId",
                )

        // 남은 경기 수 계산 (전체 대회 기준)
        val remainingGames =
            allGameTeams
                .filter { it.result == GameResult.UNDECIDED }
                .map { it.game.id }
                .distinct()
                .count()

        // 매직넘버 계산 (플레이오프 cutoff 기준)
        val magicNumbers = calculateMagicNumbers(competitionId)
        val teamMagicNumber = magicNumbers.find { it.teamId == teamId }

        return if (remainingGames > MONTE_CARLO_THRESHOLD) {
            runMonteCarloSimulation(
                currentStats = currentStats,
                targetTeamId = teamId,
                playoffTeams = playoffTeams,
                allGameTeams = allGameTeams,
                magicNumber = teamMagicNumber?.magicNumber,
            )
        } else {
            runExhaustiveSimulation(
                currentStats = currentStats,
                targetTeamId = teamId,
                playoffTeams = playoffTeams,
                allGameTeams = allGameTeams,
                magicNumber = teamMagicNumber?.magicNumber,
            )
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private fun buildTeamStatsMap(
        decidedGameTeams: List<GameTeam>,
        allGameTeams: List<GameTeam>,
    ): Map<Long, TeamStats> {
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
                gameTeams.sumOf { gt ->
                    decidedGameTeams
                        .filter { it.game.id == gt.game.id && it.team.id != teamId }
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
    }

    private fun buildStandings(statsList: List<TeamStats>): List<TeamStandingDto> {
        val sorted =
            statsList.sortedWith(
                compareByDescending<TeamStats> { it.winningPercentage }
                    .thenByDescending { it.runDifferential }
                    .thenByDescending { it.runsScored },
            )
        val leader = sorted.firstOrNull()
        return sorted.mapIndexed { index, stats ->
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
    }

    private fun calculateGamesBehind(
        leader: TeamStats?,
        team: TeamStats,
    ): BigDecimal {
        if (leader == null || leader.teamId == team.teamId) return BigDecimal.ZERO
        val winDiff = leader.wins - team.wins
        val lossDiff = team.losses - leader.losses
        return BigDecimal(winDiff + lossDiff).divide(BigDecimal(2), 1, RoundingMode.HALF_UP)
    }

    private fun validateSimulationRequest(request: SimulationRequest) {
        request.gameResults.forEach { result ->
            if (result.homeScore < 0 || result.awayScore < 0) {
                throw InvalidInputException(
                    "INVALID_SIMULATION_SCORE",
                    "점수는 0 이상이어야 합니다. gameId=${result.gameId}",
                )
            }
        }
    }

    private fun applySimulatedResults(
        currentStats: MutableMap<Long, TeamStats>,
        simulatedResults: List<SimulatedGameResult>,
        allGameTeams: List<GameTeam>,
    ): Map<Long, TeamStats> {
        val gameTeamsByGameId = allGameTeams.groupBy { it.game.id }

        simulatedResults.forEach { simResult ->
            val gameTeams = gameTeamsByGameId[simResult.gameId] ?: return@forEach
            if (gameTeams.size != 2) return@forEach

            // 이미 결과가 확정된 경기는 건너뜀
            if (gameTeams.all { it.result != GameResult.UNDECIDED }) return@forEach

            val homeTeam = gameTeams.find { it.isHome } ?: return@forEach
            val awayTeam = gameTeams.find { it.isAway } ?: return@forEach

            val homeStats = currentStats[homeTeam.team.id] ?: return@forEach
            val awayStats = currentStats[awayTeam.team.id] ?: return@forEach

            val (homeResult, awayResult) =
                when {
                    simResult.homeScore > simResult.awayScore ->
                        GameResult.WIN to GameResult.LOSS
                    simResult.homeScore < simResult.awayScore ->
                        GameResult.LOSS to GameResult.WIN
                    else -> GameResult.DRAW to GameResult.DRAW
                }

            currentStats[homeTeam.team.id] =
                homeStats.applyResult(
                    result = homeResult,
                    scored = simResult.homeScore,
                    allowed = simResult.awayScore,
                )
            currentStats[awayTeam.team.id] =
                awayStats.applyResult(
                    result = awayResult,
                    scored = simResult.awayScore,
                    allowed = simResult.homeScore,
                )
        }

        return currentStats
    }

    private fun calculateRankChanges(
        currentStandings: List<TeamStandingDto>,
        simulatedStandings: List<TeamStandingDto>,
    ): List<RankChange> {
        val currentRankMap = currentStandings.associateBy { it.teamId }
        return simulatedStandings.mapNotNull { simStanding ->
            val currentStanding = currentRankMap[simStanding.teamId] ?: return@mapNotNull null
            val rankChange = currentStanding.rank - simStanding.rank
            if (rankChange != 0) {
                RankChange(
                    teamId = simStanding.teamId,
                    teamName = simStanding.teamName,
                    previousRank = currentStanding.rank,
                    projectedRank = simStanding.rank,
                    rankChange = rankChange,
                )
            } else {
                null
            }
        }
    }

    /**
     * 몬테카를로 시뮬레이션 (남은 경기 > 15일 때 사용)
     */
    private fun runMonteCarloSimulation(
        currentStats: Map<Long, TeamStats>,
        targetTeamId: Long,
        playoffTeams: Int,
        allGameTeams: List<GameTeam>,
        magicNumber: Int?,
    ): PlayoffScenarioResult {
        val remainingGames =
            allGameTeams
                .filter { it.result == GameResult.UNDECIDED }
                .map { it.game.id }
                .distinct()
                .toList()
        val gameTeamsByGameId = allGameTeams.groupBy { it.game.id }

        var qualifyingCount = 0

        repeat(MONTE_CARLO_ITERATIONS) {
            val stats = currentStats.toMutableMap()
            remainingGames.forEach { gameId ->
                val gameTeams = gameTeamsByGameId[gameId] ?: return@forEach
                if (gameTeams.size != 2) return@forEach

                val homeTeam = gameTeams.find { it.isHome } ?: return@forEach
                val awayTeam = gameTeams.find { it.isAway } ?: return@forEach

                val rand = Random.nextDouble()
                val (homeResult, awayResult) =
                    when {
                        rand < 0.45 -> GameResult.WIN to GameResult.LOSS
                        rand < 0.90 -> GameResult.LOSS to GameResult.WIN
                        else -> GameResult.DRAW to GameResult.DRAW
                    }

                val homeStats = stats[homeTeam.team.id] ?: return@forEach
                val awayStats = stats[awayTeam.team.id] ?: return@forEach

                stats[homeTeam.team.id] =
                    homeStats.applyResult(homeResult, scored = 0, allowed = 0)
                stats[awayTeam.team.id] =
                    awayStats.applyResult(awayResult, scored = 0, allowed = 0)
            }

            val finalStandings = buildStandings(stats.values.toList())
            val teamRank = finalStandings.find { it.teamId == targetTeamId }?.rank ?: Int.MAX_VALUE
            if (teamRank <= playoffTeams) qualifyingCount++
        }

        val probability = qualifyingCount.toDouble() / MONTE_CARLO_ITERATIONS
        return PlayoffScenarioResult(
            totalScenarios = MONTE_CARLO_ITERATIONS,
            qualifyingScenarios = qualifyingCount,
            probability = probability,
            magicNumber = magicNumber,
        )
    }

    /**
     * 완전 탐색 시뮬레이션 (남은 경기 <= 15일 때 사용)
     */
    private fun runExhaustiveSimulation(
        currentStats: Map<Long, TeamStats>,
        targetTeamId: Long,
        playoffTeams: Int,
        allGameTeams: List<GameTeam>,
        magicNumber: Int?,
    ): PlayoffScenarioResult {
        val remainingGameIds =
            allGameTeams
                .filter { it.result == GameResult.UNDECIDED }
                .map { it.game.id }
                .distinct()
                .toList()

        // 경기 수가 많으면 몬테카를로로 위임
        if (remainingGameIds.size > MONTE_CARLO_THRESHOLD) {
            return runMonteCarloSimulation(
                currentStats,
                targetTeamId,
                playoffTeams,
                allGameTeams,
                magicNumber,
            )
        }

        val gameTeamsByGameId = allGameTeams.groupBy { it.game.id }
        val totalScenarios = Math.pow(3.0, remainingGameIds.size.toDouble()).toInt()
        var qualifyingCount = 0

        for (scenario in 0 until totalScenarios) {
            val stats = currentStats.toMutableMap()
            var temp = scenario

            for (gameId in remainingGameIds) {
                val outcome = temp % 3
                temp /= 3

                val gameTeams = gameTeamsByGameId[gameId] ?: continue
                if (gameTeams.size != 2) continue

                val homeTeam = gameTeams.find { it.isHome } ?: continue
                val awayTeam = gameTeams.find { it.isAway } ?: continue

                val (homeResult, awayResult) =
                    when (outcome) {
                        0 -> GameResult.WIN to GameResult.LOSS
                        1 -> GameResult.LOSS to GameResult.WIN
                        else -> GameResult.DRAW to GameResult.DRAW
                    }

                val homeStats = stats[homeTeam.team.id] ?: continue
                val awayStats = stats[awayTeam.team.id] ?: continue

                stats[homeTeam.team.id] =
                    homeStats.applyResult(homeResult, scored = 0, allowed = 0)
                stats[awayTeam.team.id] =
                    awayStats.applyResult(awayResult, scored = 0, allowed = 0)
            }

            val finalStandings = buildStandings(stats.values.toList())
            val teamRank = finalStandings.find { it.teamId == targetTeamId }?.rank ?: Int.MAX_VALUE
            if (teamRank <= playoffTeams) qualifyingCount++
        }

        val probability =
            if (totalScenarios > 0) qualifyingCount.toDouble() / totalScenarios else 0.0
        return PlayoffScenarioResult(
            totalScenarios = totalScenarios,
            qualifyingScenarios = qualifyingCount,
            probability = probability,
            magicNumber = magicNumber,
        )
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
    ) {
        fun applyResult(
            result: GameResult,
            scored: Int,
            allowed: Int,
        ): TeamStats {
            val newWins = wins + if (result == GameResult.WIN) 1 else 0
            val newLosses = losses + if (result == GameResult.LOSS) 1 else 0
            val newDraws = draws + if (result == GameResult.DRAW) 1 else 0
            val newGamesPlayed = newWins + newLosses + newDraws
            val newRunsScored = runsScored + scored
            val newRunsAllowed = runsAllowed + allowed
            val newRunDifferential = newRunsScored - newRunsAllowed
            val newWinningPercentage =
                if (newGamesPlayed > 0) {
                    BigDecimal(newWins)
                        .add(BigDecimal(newDraws).multiply(BigDecimal("0.5")))
                        .divide(BigDecimal(newGamesPlayed), 3, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }
            return copy(
                gamesPlayed = newGamesPlayed,
                remainingGames = maxOf(0, remainingGames - 1),
                wins = newWins,
                losses = newLosses,
                draws = newDraws,
                winningPercentage = newWinningPercentage,
                runsScored = newRunsScored,
                runsAllowed = newRunsAllowed,
                runDifferential = newRunDifferential,
            )
        }
    }
}
