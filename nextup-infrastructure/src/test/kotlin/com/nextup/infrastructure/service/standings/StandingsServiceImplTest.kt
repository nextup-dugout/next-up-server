package com.nextup.infrastructure.service.standings

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("StandingsServiceImpl")
class StandingsServiceImplTest {
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var standingsService: StandingsServiceImpl

    private lateinit var competition: Competition
    private lateinit var league: League
    private lateinit var association: Association

    @BeforeEach
    fun setUp() {
        competitionRepository = mockk()
        gameTeamRepository = mockk()
        standingsService = StandingsServiceImpl(competitionRepository, gameTeamRepository)

        // Test data setup
        association = createAssociation(1L, "서울시야구협회")
        league = createLeague(1L, "1부 리그", association)
        competition = createCompetition(1L, league, "2025 춘계대회", 2025, 1)
    }

    @Nested
    @DisplayName("getStandings")
    inner class GetStandings {
        @Test
        fun `대회가 존재하지 않으면 CompetitionNotFoundException 발생`() {
            // given
            val competitionId = 999L
            every { competitionRepository.findByIdOrNull(competitionId) } returns null

            // when & then
            assertThrows<CompetitionNotFoundException> {
                standingsService.getStandings(competitionId)
            }
        }

        @Test
        fun `경기 기록이 없으면 빈 순위표 반환`() {
            // given
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns emptyList()
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns emptyList()

            // when
            val result = standingsService.getStandings(1L)

            // then
            assertThat(result.competitionId).isEqualTo(1L)
            assertThat(result.competitionName).isEqualTo("2025 춘계대회")
            assertThat(result.standings).isEmpty()
            assertThat(result.totalGamesPerTeam).isEqualTo(0)
        }

        @Test
        fun `팀들을 승률 순으로 정렬`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")
            val teamC = createTeam(3L, league, "Bears")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            // Tigers: 2승 0패 (승률 1.000)
            val teamA1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val teamB1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 3, GameResult.LOSS)
            val teamA2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 7, GameResult.WIN)
            val teamC1 = createGameTeam(4L, game2, teamC, HomeAway.AWAY, 2, GameResult.LOSS)

            val allGameTeams = listOf(teamA1, teamB1, teamA2, teamC1)
            val decidedGameTeams = listOf(teamA1, teamB1, teamA2, teamC1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = standingsService.getStandings(1L)

            // then
            assertThat(result.standings).hasSize(3)
            assertThat(result.standings[0].teamName).isEqualTo("Tigers")
            assertThat(result.standings[0].rank).isEqualTo(1)
            assertThat(result.standings[0].wins).isEqualTo(2)
            assertThat(result.standings[0].losses).isEqualTo(0)
            assertThat(result.standings[0].winningPercentage).isEqualByComparingTo(BigDecimal("1.000"))
        }

        @Test
        fun `승률 동률 시 득실점차로 정렬`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            // Tigers: 1승 1패, 득점 7 (5+2), 실점 7 (3+4), 득실차 0
            // Lions: 1승 1패, 득점 7 (3+4), 실점 7 (5+2), 득실차 0
            // But Tigers 득점이 더 많으므로 Tigers가 1위 (득점순 정렬)
            val teamA1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val teamB1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 3, GameResult.LOSS)
            val teamA2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 2, GameResult.LOSS)
            val teamB2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 4, GameResult.WIN)

            val allGameTeams = listOf(teamA1, teamB1, teamA2, teamB2)
            val decidedGameTeams = listOf(teamA1, teamB1, teamA2, teamB2)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = standingsService.getStandings(1L)

            // then
            assertThat(result.standings).hasSize(2)
            // 둘 다 승률 0.500, 득실차도 0이므로 득점으로 정렬 (둘 다 7점이므로 순서는 teamId 순)
            assertThat(result.standings[0].runDifferential).isEqualTo(0)
            assertThat(result.standings[1].runDifferential).isEqualTo(0)
        }

        @Test
        fun `승차(gamesBehind)를 올바르게 계산`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")
            val teamC = createTeam(3L, league, "Bears")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)
            val game3 = createGame(3L, competition)
            val game4 = createGame(4L, competition)

            // Tigers: 3승 0패
            val teamA1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val teamB1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 3, GameResult.LOSS)
            val teamA2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 7, GameResult.WIN)
            val teamC1 = createGameTeam(4L, game2, teamC, HomeAway.AWAY, 2, GameResult.LOSS)
            val teamA3 = createGameTeam(5L, game3, teamA, HomeAway.HOME, 4, GameResult.WIN)
            val teamB2 = createGameTeam(6L, game3, teamB, HomeAway.AWAY, 3, GameResult.LOSS)

            // Lions: 1승 2패 -> 승차 = ((3-1) + (2-0)) / 2 = (2+2)/2 = 2.0
            val teamB3 = createGameTeam(7L, game4, teamB, HomeAway.HOME, 6, GameResult.WIN)
            val teamC2 = createGameTeam(8L, game4, teamC, HomeAway.AWAY, 4, GameResult.LOSS)

            // Bears: 0승 3패 -> 승차 = ((3-0) + (3-0)) / 2 = (3+3)/2 = 3.0 (X)
            // 실제: Bears는 0승 2패 (Tigers와 2경기, Lions와 1경기)
            // 승차 = ((3-0) + (2-0)) / 2 = 5/2 = 2.5

            val allGameTeams = listOf(teamA1, teamB1, teamA2, teamC1, teamA3, teamB2, teamB3, teamC2)
            val decidedGameTeams = listOf(teamA1, teamB1, teamA2, teamC1, teamA3, teamB2, teamB3, teamC2)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = standingsService.getStandings(1L)

            // then
            assertThat(result.standings[0].teamName).isEqualTo("Tigers")
            assertThat(result.standings[0].wins).isEqualTo(3)
            assertThat(result.standings[0].losses).isEqualTo(0)
            assertThat(result.standings[0].gamesBehind).isEqualByComparingTo(BigDecimal("0.0"))

            assertThat(result.standings[1].teamName).isEqualTo("Lions")
            assertThat(result.standings[1].wins).isEqualTo(1)
            assertThat(result.standings[1].losses).isEqualTo(2)
            assertThat(result.standings[1].gamesBehind).isEqualByComparingTo(BigDecimal("2.0"))

            assertThat(result.standings[2].teamName).isEqualTo("Bears")
            assertThat(result.standings[2].wins).isEqualTo(0)
            assertThat(result.standings[2].losses).isEqualTo(2)
            assertThat(result.standings[2].gamesBehind).isEqualByComparingTo(BigDecimal("2.5"))
        }

        @Test
        fun `남은 경기 수(remainingGames)를 올바르게 계산`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)
            val game3 = createGame(3L, competition)

            // Tigers: 2경기 완료, 1경기 예정
            val teamA1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val teamB1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 3, GameResult.LOSS)
            val teamA2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 7, GameResult.WIN)
            val teamB2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 2, GameResult.LOSS)
            val teamA3 = createGameTeam(5L, game3, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val teamB3 = createGameTeam(6L, game3, teamB, HomeAway.AWAY, 0, GameResult.UNDECIDED)

            val allGameTeams = listOf(teamA1, teamB1, teamA2, teamB2, teamA3, teamB3)
            val decidedGameTeams = listOf(teamA1, teamB1, teamA2, teamB2) // 확정된 경기만

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = standingsService.getStandings(1L)

            // then
            assertThat(result.standings[0].gamesPlayed).isEqualTo(2)
            assertThat(result.standings[0].remainingGames).isEqualTo(1)
            assertThat(result.standings[1].gamesPlayed).isEqualTo(2)
            assertThat(result.standings[1].remainingGames).isEqualTo(1)
            assertThat(result.totalGamesPerTeam).isEqualTo(3)
        }

        @Test
        fun `무승부를 0점5승으로 계산하여 승률 산출`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            // Tigers: 1승 0패 1무 -> 승률 = (1 + 0.5) / 2 = 0.750
            val teamA1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val teamB1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 3, GameResult.LOSS)
            val teamA2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 3, GameResult.DRAW)
            val teamB2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 3, GameResult.DRAW)

            val allGameTeams = listOf(teamA1, teamB1, teamA2, teamB2)
            val decidedGameTeams = listOf(teamA1, teamB1, teamA2, teamB2)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = standingsService.getStandings(1L)

            // then
            assertThat(result.standings[0].teamName).isEqualTo("Tigers")
            assertThat(result.standings[0].wins).isEqualTo(1)
            assertThat(result.standings[0].losses).isEqualTo(0)
            assertThat(result.standings[0].draws).isEqualTo(1)
            assertThat(result.standings[0].winningPercentage).isEqualByComparingTo(BigDecimal("0.750"))

            assertThat(result.standings[1].teamName).isEqualTo("Lions")
            assertThat(result.standings[1].wins).isEqualTo(0)
            assertThat(result.standings[1].losses).isEqualTo(1)
            assertThat(result.standings[1].draws).isEqualTo(1)
            assertThat(result.standings[1].winningPercentage).isEqualByComparingTo(BigDecimal("0.250"))
        }

        @Test
        fun `득점과 실점을 올바르게 계산`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            // Tigers: 득점 12 (5+7), 실점 5 (3+2)
            val teamA1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val teamB1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 3, GameResult.LOSS)
            val teamA2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 7, GameResult.WIN)
            val teamB2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            val allGameTeams = listOf(teamA1, teamB1, teamA2, teamB2)
            val decidedGameTeams = listOf(teamA1, teamB1, teamA2, teamB2)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = standingsService.getStandings(1L)

            // then
            assertThat(result.standings[0].teamName).isEqualTo("Tigers")
            assertThat(result.standings[0].runsScored).isEqualTo(12)
            assertThat(result.standings[0].runsAllowed).isEqualTo(5)
            assertThat(result.standings[0].runDifferential).isEqualTo(7)

            assertThat(result.standings[1].teamName).isEqualTo("Lions")
            assertThat(result.standings[1].runsScored).isEqualTo(5)
            assertThat(result.standings[1].runsAllowed).isEqualTo(12)
            assertThat(result.standings[1].runDifferential).isEqualTo(-7)
        }

        @Test
        fun `아직 경기 결과가 없는 팀도 0승 0패로 순위표에 포함`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")
            val teamC = createTeam(3L, league, "Bears") // 아직 경기 결과 없음

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition) // Bears 경기 (예정)

            // Tigers vs Lions 경기 (결과 확정)
            val teamA1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val teamB1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 3, GameResult.LOSS)

            // Bears 경기 (예정 - UNDECIDED)
            val teamA2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val teamC1 = createGameTeam(4L, game2, teamC, HomeAway.AWAY, 0, GameResult.UNDECIDED)

            // allGameTeams에는 모든 팀이 포함
            val allGameTeams = listOf(teamA1, teamB1, teamA2, teamC1)
            // decidedGameTeams에는 Bears가 없음 (아직 결과 확정 경기 없음)
            val decidedGameTeams = listOf(teamA1, teamB1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = standingsService.getStandings(1L)

            // then
            assertThat(result.standings).hasSize(3)

            // Bears는 0승 0패 0무로 포함되어야 함
            val bearsStanding = result.standings.find { it.teamName == "Bears" }
            assertThat(bearsStanding).isNotNull
            assertThat(bearsStanding!!.wins).isEqualTo(0)
            assertThat(bearsStanding.losses).isEqualTo(0)
            assertThat(bearsStanding.draws).isEqualTo(0)
            assertThat(bearsStanding.gamesPlayed).isEqualTo(0)
            assertThat(bearsStanding.remainingGames).isEqualTo(1) // 예정된 경기 1개
            assertThat(bearsStanding.winningPercentage).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(bearsStanding.runsScored).isEqualTo(0)
            assertThat(bearsStanding.runsAllowed).isEqualTo(0)
            assertThat(bearsStanding.runDifferential).isEqualTo(0)

            // Bears는 승률 0이지만 득실점차 0으로 Lions(-2)보다 높은 2위
            // 순위: Tigers(승률 1.0) > Bears(승률 0, 득실 0) > Lions(승률 0, 득실 -2)
            assertThat(result.standings[0].teamName).isEqualTo("Tigers")
            assertThat(result.standings[1].teamName).isEqualTo("Bears")
            assertThat(result.standings[2].teamName).isEqualTo("Lions")
        }
    }

    // Helper methods
    private fun createAssociation(
        id: Long,
        name: String,
    ): Association =
        Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null,
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association,
    ): League =
        League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null,
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createCompetition(
        id: Long,
        league: League,
        name: String,
        year: Int,
        season: Int,
    ): Competition =
        Competition(
            league = league,
            name = name,
            year = year,
            season = season,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(year, 3, 1),
            endDate = null,
            description = null,
            maxTeams = null,
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createTeam(
        id: Long,
        league: League,
        name: String,
    ): Team =
        Team(
            league = league,
            name = name,
            city = "서울",
            foundedYear = 2020,
            abbreviation = null,
            logoUrl = null,
        ).apply {
            val idField = Team::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createGame(
        id: Long,
        competition: Competition,
    ): Game =
        Game(
            competition = competition,
            scheduledAt = LocalDateTime.now(),
            location = "서울야구장",
            status = GameStatus.SCHEDULED,
        ).apply {
            val idField = Game::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createGameTeam(
        id: Long,
        game: Game,
        team: Team,
        homeAway: HomeAway,
        totalScore: Int,
        result: GameResult,
    ): GameTeam =
        GameTeam(
            game = game,
            team = team,
            homeAway = homeAway,
            id = id,
        ).apply {
            updateScore(totalScore, 0, 0)
            updateResult(result)
        }
}
