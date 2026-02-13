package com.nextup.infrastructure.service.team

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.game.GameResult
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.team.TeamMatchupService
import com.nextup.core.service.team.dto.TeamMatchupDto
import com.nextup.core.service.team.dto.TeamMatchupGameDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional(readOnly = true)
class TeamMatchupServiceImpl(
    private val gameTeamRepositoryPort: GameTeamRepositoryPort,
    private val teamRepositoryPort: TeamRepositoryPort,
) : TeamMatchupService {
    override fun getTeamMatchup(
        teamId: Long,
        opponentId: Long,
        competitionId: Long?,
    ): TeamMatchupDto {
        // 팀 존재 여부 확인
        val team =
            teamRepositoryPort.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)
        val opponent =
            teamRepositoryPort.findByIdOrNull(opponentId)
                ?: throw TeamNotFoundException(opponentId)

        // 두 팀 간의 완료된 경기 조회
        val teamGameTeams =
            gameTeamRepositoryPort.findCompletedGamesBetweenTeams(
                teamId,
                opponentId,
                competitionId,
            )

        // 통계 계산
        var wins = 0
        var losses = 0
        var draws = 0
        var runsScored = 0
        var runsAllowed = 0

        teamGameTeams.forEach { teamGameTeam ->
            when (teamGameTeam.result) {
                GameResult.WIN -> wins++
                GameResult.LOSS -> losses++
                GameResult.DRAW -> draws++
                GameResult.UNDECIDED -> {} // 완료된 경기만 조회했으므로 발생하지 않음
            }

            runsScored += teamGameTeam.totalScore

            // 상대팀의 점수 찾기
            val opponentGameTeam =
                gameTeamRepositoryPort
                    .findAllByGameId(teamGameTeam.game.id)
                    .firstOrNull { it.team.id == opponentId }

            if (opponentGameTeam != null) {
                runsAllowed += opponentGameTeam.totalScore
            }
        }

        val totalGames = wins + losses + draws

        // 평균 득실점 계산
        val avgRunsScored =
            if (totalGames > 0) {
                BigDecimal(runsScored)
                    .divide(BigDecimal(totalGames), 2, RoundingMode.HALF_UP)
                    .toDouble()
            } else {
                0.0
            }

        val avgRunsAllowed =
            if (totalGames > 0) {
                BigDecimal(runsAllowed)
                    .divide(BigDecimal(totalGames), 2, RoundingMode.HALF_UP)
                    .toDouble()
            } else {
                0.0
            }

        return TeamMatchupDto(
            teamId = teamId,
            teamName = team.fullName,
            opponentId = opponentId,
            opponentName = opponent.fullName,
            wins = wins,
            losses = losses,
            draws = draws,
            totalGames = totalGames,
            runsScored = runsScored,
            runsAllowed = runsAllowed,
            avgRunsScored = avgRunsScored,
            avgRunsAllowed = avgRunsAllowed,
        )
    }

    override fun getRecentGames(
        teamId: Long,
        opponentId: Long,
        limit: Int,
    ): List<TeamMatchupGameDto> {
        // 팀 존재 여부 확인
        teamRepositoryPort.findByIdOrNull(teamId)
            ?: throw TeamNotFoundException(teamId)
        teamRepositoryPort.findByIdOrNull(opponentId)
            ?: throw TeamNotFoundException(opponentId)

        // 최근 경기 조회
        val teamGameTeams =
            gameTeamRepositoryPort
                .findCompletedGamesBetweenTeams(teamId, opponentId, null)
                .take(limit)

        return teamGameTeams.map { teamGameTeam ->
            val game = teamGameTeam.game
            val allGameTeams = gameTeamRepositoryPort.findAllByGameId(game.id)

            val homeTeam = allGameTeams.firstOrNull { it.isHome }
            val awayTeam = allGameTeams.firstOrNull { it.isAway }

            // 결과 결정 (teamId 관점에서)
            val result =
                when (teamGameTeam.result) {
                    GameResult.WIN -> "WIN"
                    GameResult.LOSS -> "LOSS"
                    GameResult.DRAW -> "DRAW"
                    GameResult.UNDECIDED -> "UNDECIDED"
                }

            TeamMatchupGameDto(
                gameId = game.id,
                date = game.scheduledAt.toLocalDate(),
                homeTeamId = homeTeam?.team?.id ?: 0L,
                awayTeamId = awayTeam?.team?.id ?: 0L,
                homeScore = homeTeam?.totalScore ?: 0,
                awayScore = awayTeam?.totalScore ?: 0,
                result = result,
                venue = game.location,
            )
        }
    }
}
