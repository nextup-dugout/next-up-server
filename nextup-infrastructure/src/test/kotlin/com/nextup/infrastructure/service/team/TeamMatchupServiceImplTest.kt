package com.nextup.infrastructure.service.team

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class TeamMatchupServiceImplTest {
    private val gameTeamRepositoryPort: GameTeamRepositoryPort = mockk()
    private val teamRepositoryPort: TeamRepositoryPort = mockk()
    private val service = TeamMatchupServiceImpl(gameTeamRepositoryPort, teamRepositoryPort)

    private val association =
        Association(
            name = "테스트 협회",
            abbreviation = "TA",
            region = "서울",
            id = 1L,
        )

    private val league =
        League(
            association = association,
            name = "테스트 리그",
            abbreviation = "TL",
            foundedYear = 2020,
            id = 1L,
        )

    private val competition =
        Competition(
            league = league,
            name = "2024 시즌",
            year = 2024,
            season = 1,
            startDate = LocalDate.now().minusMonths(3),
            endDate = LocalDate.now().plusMonths(3),
            id = 1L,
        )

    private val teamA =
        Team(
            league = league,
            name = "타이거즈",
            city = "서울",
            foundedYear = 2020,
            id = 1L,
        )

    private val teamB =
        Team(
            league = league,
            name = "라이온즈",
            city = "부산",
            foundedYear = 2020,
            id = 2L,
        )

    @Test
    fun `should return team matchup with correct win-loss record`() {
        // given
        val game1 = createGame(1L, GameStatus.FINISHED)
        val game2 = createGame(2L, GameStatus.FINISHED)
        val game3 = createGame(3L, GameStatus.FINISHED)

        val teamAGameTeams =
            listOf(
                createGameTeam(game1, teamA, HomeAway.HOME, GameResult.WIN, 5, 3),
                createGameTeam(game2, teamA, HomeAway.AWAY, GameResult.LOSS, 2, 4),
                createGameTeam(game3, teamA, HomeAway.HOME, GameResult.WIN, 7, 1),
            )

        every { teamRepositoryPort.findByIdOrNull(teamA.id) } returns teamA
        every { teamRepositoryPort.findByIdOrNull(teamB.id) } returns teamB
        every {
            gameTeamRepositoryPort.findCompletedGamesBetweenTeams(
                teamA.id,
                teamB.id,
                null,
            )
        } returns teamAGameTeams

        every { gameTeamRepositoryPort.findAllByGameId(game1.id) } returns
            listOf(
                teamAGameTeams[0],
                createGameTeam(game1, teamB, HomeAway.AWAY, GameResult.LOSS, 3, 5),
            )
        every { gameTeamRepositoryPort.findAllByGameId(game2.id) } returns
            listOf(
                teamAGameTeams[1],
                createGameTeam(game2, teamB, HomeAway.HOME, GameResult.WIN, 4, 2),
            )
        every { gameTeamRepositoryPort.findAllByGameId(game3.id) } returns
            listOf(
                teamAGameTeams[2],
                createGameTeam(game3, teamB, HomeAway.AWAY, GameResult.LOSS, 1, 7),
            )

        // when
        val result = service.getTeamMatchup(teamA.id, teamB.id, null)

        // then
        assertThat(result.teamId).isEqualTo(teamA.id)
        assertThat(result.teamName).isEqualTo("서울 타이거즈")
        assertThat(result.opponentId).isEqualTo(teamB.id)
        assertThat(result.opponentName).isEqualTo("부산 라이온즈")
        assertThat(result.wins).isEqualTo(2)
        assertThat(result.losses).isEqualTo(1)
        assertThat(result.draws).isEqualTo(0)
        assertThat(result.totalGames).isEqualTo(3)
        assertThat(result.runsScored).isEqualTo(14) // 5 + 2 + 7
        assertThat(result.runsAllowed).isEqualTo(8) // 3 + 4 + 1
        assertThat(result.avgRunsScored).isEqualTo(4.67)
        assertThat(result.avgRunsAllowed).isEqualTo(2.67)
    }

    @Test
    fun `should return empty matchup when no games played`() {
        // given
        every { teamRepositoryPort.findByIdOrNull(teamA.id) } returns teamA
        every { teamRepositoryPort.findByIdOrNull(teamB.id) } returns teamB
        every {
            gameTeamRepositoryPort.findCompletedGamesBetweenTeams(
                teamA.id,
                teamB.id,
                null,
            )
        } returns emptyList()

        // when
        val result = service.getTeamMatchup(teamA.id, teamB.id, null)

        // then
        assertThat(result.totalGames).isEqualTo(0)
        assertThat(result.wins).isEqualTo(0)
        assertThat(result.losses).isEqualTo(0)
        assertThat(result.draws).isEqualTo(0)
        assertThat(result.runsScored).isEqualTo(0)
        assertThat(result.runsAllowed).isEqualTo(0)
        assertThat(result.avgRunsScored).isEqualTo(0.0)
        assertThat(result.avgRunsAllowed).isEqualTo(0.0)
    }

    @Test
    fun `should handle draw games correctly`() {
        // given
        val game1 = createGame(1L, GameStatus.FINISHED)

        val teamAGameTeams =
            listOf(
                createGameTeam(game1, teamA, HomeAway.HOME, GameResult.DRAW, 3, 3),
            )

        every { teamRepositoryPort.findByIdOrNull(teamA.id) } returns teamA
        every { teamRepositoryPort.findByIdOrNull(teamB.id) } returns teamB
        every {
            gameTeamRepositoryPort.findCompletedGamesBetweenTeams(
                teamA.id,
                teamB.id,
                null,
            )
        } returns teamAGameTeams

        every { gameTeamRepositoryPort.findAllByGameId(game1.id) } returns
            listOf(
                teamAGameTeams[0],
                createGameTeam(game1, teamB, HomeAway.AWAY, GameResult.DRAW, 3, 3),
            )

        // when
        val result = service.getTeamMatchup(teamA.id, teamB.id, null)

        // then
        assertThat(result.wins).isEqualTo(0)
        assertThat(result.losses).isEqualTo(0)
        assertThat(result.draws).isEqualTo(1)
        assertThat(result.totalGames).isEqualTo(1)
    }

    @Test
    fun `should throw TeamNotFoundException when team does not exist`() {
        // given
        every { teamRepositoryPort.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<TeamNotFoundException> {
            service.getTeamMatchup(999L, teamB.id, null)
        }
    }

    @Test
    fun `should throw TeamNotFoundException when opponent does not exist`() {
        // given
        every { teamRepositoryPort.findByIdOrNull(teamA.id) } returns teamA
        every { teamRepositoryPort.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<TeamNotFoundException> {
            service.getTeamMatchup(teamA.id, 999L, null)
        }
    }

    @Test
    fun `should return recent games in descending order`() {
        // given
        val game1 = createGame(1L, GameStatus.FINISHED, LocalDateTime.now().minusDays(10))
        val game2 = createGame(2L, GameStatus.FINISHED, LocalDateTime.now().minusDays(5))
        val game3 = createGame(3L, GameStatus.FINISHED, LocalDateTime.now().minusDays(1))

        val teamAGameTeams =
            listOf(
                createGameTeam(game3, teamA, HomeAway.HOME, GameResult.WIN, 5, 3),
                createGameTeam(game2, teamA, HomeAway.AWAY, GameResult.LOSS, 2, 4),
                createGameTeam(game1, teamA, HomeAway.HOME, GameResult.WIN, 7, 1),
            )

        every { teamRepositoryPort.findByIdOrNull(teamA.id) } returns teamA
        every { teamRepositoryPort.findByIdOrNull(teamB.id) } returns teamB
        every {
            gameTeamRepositoryPort.findCompletedGamesBetweenTeams(
                teamA.id,
                teamB.id,
                null,
            )
        } returns teamAGameTeams

        every { gameTeamRepositoryPort.findAllByGameId(game3.id) } returns
            listOf(
                teamAGameTeams[0],
                createGameTeam(game3, teamB, HomeAway.AWAY, GameResult.LOSS, 3, 5),
            )
        every { gameTeamRepositoryPort.findAllByGameId(game2.id) } returns
            listOf(
                teamAGameTeams[1],
                createGameTeam(game2, teamB, HomeAway.HOME, GameResult.WIN, 4, 2),
            )
        every { gameTeamRepositoryPort.findAllByGameId(game1.id) } returns
            listOf(
                teamAGameTeams[2],
                createGameTeam(game1, teamB, HomeAway.AWAY, GameResult.LOSS, 1, 7),
            )

        // when
        val result = service.getRecentGames(teamA.id, teamB.id, 10)

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0].gameId).isEqualTo(game3.id)
        assertThat(result[1].gameId).isEqualTo(game2.id)
        assertThat(result[2].gameId).isEqualTo(game1.id)
    }

    @Test
    fun `should limit recent games to specified limit`() {
        // given
        val game1 = createGame(1L, GameStatus.FINISHED, LocalDateTime.now().minusDays(10))
        val game2 = createGame(2L, GameStatus.FINISHED, LocalDateTime.now().minusDays(5))
        val game3 = createGame(3L, GameStatus.FINISHED, LocalDateTime.now().minusDays(1))

        val teamAGameTeams =
            listOf(
                createGameTeam(game3, teamA, HomeAway.HOME, GameResult.WIN, 5, 3),
                createGameTeam(game2, teamA, HomeAway.AWAY, GameResult.LOSS, 2, 4),
                createGameTeam(game1, teamA, HomeAway.HOME, GameResult.WIN, 7, 1),
            )

        every { teamRepositoryPort.findByIdOrNull(teamA.id) } returns teamA
        every { teamRepositoryPort.findByIdOrNull(teamB.id) } returns teamB
        every {
            gameTeamRepositoryPort.findCompletedGamesBetweenTeams(
                teamA.id,
                teamB.id,
                null,
            )
        } returns teamAGameTeams

        every { gameTeamRepositoryPort.findAllByGameId(game3.id) } returns
            listOf(
                teamAGameTeams[0],
                createGameTeam(game3, teamB, HomeAway.AWAY, GameResult.LOSS, 3, 5),
            )
        every { gameTeamRepositoryPort.findAllByGameId(game2.id) } returns
            listOf(
                teamAGameTeams[1],
                createGameTeam(game2, teamB, HomeAway.HOME, GameResult.WIN, 4, 2),
            )

        // when
        val result = service.getRecentGames(teamA.id, teamB.id, 2)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].gameId).isEqualTo(game3.id)
        assertThat(result[1].gameId).isEqualTo(game2.id)
    }

    @Test
    fun `should return correct result from team perspective`() {
        // given
        val game1 = createGame(1L, GameStatus.FINISHED)

        val teamAGameTeams =
            listOf(
                createGameTeam(game1, teamA, HomeAway.HOME, GameResult.WIN, 5, 3),
            )

        every { teamRepositoryPort.findByIdOrNull(teamA.id) } returns teamA
        every { teamRepositoryPort.findByIdOrNull(teamB.id) } returns teamB
        every {
            gameTeamRepositoryPort.findCompletedGamesBetweenTeams(
                teamA.id,
                teamB.id,
                null,
            )
        } returns teamAGameTeams

        every { gameTeamRepositoryPort.findAllByGameId(game1.id) } returns
            listOf(
                teamAGameTeams[0],
                createGameTeam(game1, teamB, HomeAway.AWAY, GameResult.LOSS, 3, 5),
            )

        // when
        val result = service.getRecentGames(teamA.id, teamB.id, 10)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].result).isEqualTo("WIN")
        assertThat(result[0].homeTeamId).isEqualTo(teamA.id)
        assertThat(result[0].awayTeamId).isEqualTo(teamB.id)
        assertThat(result[0].homeScore).isEqualTo(5)
        assertThat(result[0].awayScore).isEqualTo(3)
    }

    private fun createGame(
        id: Long,
        status: GameStatus,
        scheduledAt: LocalDateTime = LocalDateTime.now(),
    ): Game =
        Game(
            competition = competition,
            scheduledAt = scheduledAt,
            status = status,
            id = id,
        )

    private fun createGameTeam(
        game: Game,
        team: Team,
        homeAway: HomeAway,
        result: GameResult,
        totalScore: Int,
        opponentScore: Int,
    ): GameTeam {
        val gameTeam =
            GameTeam(
                game = game,
                team = team,
                homeAway = homeAway,
                id = 0L,
            )
        gameTeam.updateScore(totalScore, 0, 0)
        gameTeam.updateResult(result)
        return gameTeam
    }
}
