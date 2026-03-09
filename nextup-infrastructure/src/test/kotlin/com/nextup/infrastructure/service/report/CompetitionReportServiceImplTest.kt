package com.nextup.infrastructure.service.report

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.standings.StandingsService
import com.nextup.core.service.standings.dto.StandingsDto
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class CompetitionReportServiceImplTest {
    private val competitionRepository: CompetitionRepositoryPort = mockk()
    private val standingsService: StandingsService = mockk()
    private val gameRepository: GameRepositoryPort = mockk()
    private val gameTeamRepository: GameTeamRepositoryPort = mockk()
    private val battingRecordRepository: BattingRecordRepositoryPort = mockk()
    private val pitchingRecordRepository: PitchingRecordRepositoryPort = mockk()

    private val service =
        CompetitionReportServiceImpl(
            competitionRepository = competitionRepository,
            standingsService = standingsService,
            gameRepository = gameRepository,
            gameTeamRepository = gameTeamRepository,
            battingRecordRepository = battingRecordRepository,
            pitchingRecordRepository = pitchingRecordRepository,
        )

    @Test
    fun `should throw exception when competition not found`() {
        // given
        val competitionId = 1L
        every { competitionRepository.findByIdOrNull(competitionId) } returns null

        // when & then
        assertThrows<CompetitionNotFoundException> {
            service.getReport(competitionId)
        }
    }

    @Test
    fun `should return report with standings and summary`() {
        // given
        val competitionId = 1L
        val league = mockk<League>(relaxed = true)
        val competition =
            Competition(
                league = league,
                name = "2024 춘계대회",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
                endDate = LocalDate.of(2024, 5, 31),
                status = CompetitionStatus.IN_PROGRESS,
                id = competitionId,
            )

        val standingsDto =
            StandingsDto(
                competitionId = competitionId,
                competitionName = "2024 춘계대회",
                totalGamesPerTeam = 10,
                standings = emptyList(),
                lastUpdated = LocalDateTime.now(),
            )

        every { competitionRepository.findByIdOrNull(competitionId) } returns competition
        every { standingsService.getStandings(competitionId) } returns standingsDto
        every { gameRepository.findByCompetitionId(competitionId) } returns emptyList()
        every { battingRecordRepository.findAllByGameId(any()) } returns emptyList()
        every { pitchingRecordRepository.findAllByGameId(any()) } returns emptyList()
        every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(competitionId) } returns emptyList()

        // when
        val result = service.getReport(competitionId)

        // then
        assertThat(result.competitionId).isEqualTo(competitionId)
        assertThat(result.competitionName).isEqualTo("2024 춘계대회")
        assertThat(result.season).isEqualTo(1)
        assertThat(result.standings).isEmpty()
        assertThat(result.summary.totalGames).isEqualTo(0)
    }

    @Test
    fun `should calculate summary statistics correctly`() {
        // given
        val competitionId = 1L
        val league = mockk<League>(relaxed = true)
        val competition =
            Competition(
                league = league,
                name = "2024 춘계대회",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
                id = competitionId,
            )

        val team1 =
            mockk<Team>(relaxed = true) {
                every { id } returns 1L
                every { name } returns "팀A"
            }
        val team2 =
            mockk<Team>(relaxed = true) {
                every { id } returns 2L
                every { name } returns "팀B"
            }
        val fixtureTeamA =
            Team(
                league =
                    com.nextup.core.domain.league.League(
                        association = com.nextup.core.domain.association.Association(name = "협회", region = "서울"),
                        name = "리그",
                        foundedYear = 2020,
                    ),
                name = "홈팀",
                city = "서울",
                foundedYear = 2020,
                id = 10L,
            )
        val fixtureTeamB =
            Team(
                league =
                    com.nextup.core.domain.league.League(
                        association = com.nextup.core.domain.association.Association(name = "협회", region = "서울"),
                        name = "리그",
                        foundedYear = 2020,
                    ),
                name = "원정팀",
                city = "부산",
                foundedYear = 2020,
                id = 11L,
            )

        val game1 =
            Game.createForTest(
                competition = competition,
                homeTeam = fixtureTeamA,
                awayTeam = fixtureTeamB,
                scheduledAt = LocalDateTime.of(2024, 3, 15, 14, 0),
                status = GameStatus.FINISHED,
                id = 1L,
            )

        val game2 =
            Game.createForTest(
                competition = competition,
                homeTeam = fixtureTeamA,
                awayTeam = fixtureTeamB,
                scheduledAt = LocalDateTime.of(2024, 3, 20, 14, 0),
                status = GameStatus.FINISHED,
                id = 2L,
            )

        val gameTeam1 =
            GameTeam(game = game1, team = team1, homeAway = HomeAway.HOME, id = 1L).apply {
                updateScore(totalScore = 5, totalHits = 8, totalErrors = 1)
                updateResult(GameResult.WIN)
            }

        val gameTeam2 =
            GameTeam(game = game1, team = team2, homeAway = HomeAway.AWAY, id = 2L).apply {
                updateScore(totalScore = 3, totalHits = 6, totalErrors = 2)
                updateResult(GameResult.LOSS)
            }

        val gameTeam3 =
            GameTeam(game = game2, team = team1, homeAway = HomeAway.AWAY, id = 3L).apply {
                updateScore(totalScore = 7, totalHits = 10, totalErrors = 0)
                updateResult(GameResult.WIN)
            }

        val gameTeam4 =
            GameTeam(game = game2, team = team2, homeAway = HomeAway.HOME, id = 4L).apply {
                updateScore(totalScore = 2, totalHits = 4, totalErrors = 1)
                updateResult(GameResult.LOSS)
            }

        every { competitionRepository.findByIdOrNull(competitionId) } returns competition
        every { gameRepository.findByCompetitionId(competitionId) } returns listOf(game1, game2)
        every { gameTeamRepository.findAllByGameIds(listOf(1L, 2L)) } returns
            listOf(gameTeam1, gameTeam2, gameTeam3, gameTeam4)
        every { battingRecordRepository.findAllByGameId(any()) } returns emptyList()
        every { pitchingRecordRepository.findAllByGameId(any()) } returns emptyList()
        every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(competitionId) } returns
            listOf(gameTeam1, gameTeam2, gameTeam3, gameTeam4)

        // when
        val result = service.getReportSummary(competitionId)

        // then
        assertThat(result.competitionId).isEqualTo(competitionId)
        assertThat(result.totalGames).isEqualTo(2)
        assertThat(result.completedGames).isEqualTo(2)
        assertThat(result.totalRuns).isEqualTo(17) // 5 + 3 + 7 + 2
        assertThat(result.totalHits).isEqualTo(28) // 8 + 6 + 10 + 4
        assertThat(result.averageRunsPerGame).isEqualTo(8.5) // 17 / 2
        assertThat(result.highestScoringGame).isNotNull
        assertThat(result.highestScoringGame?.totalRuns).isEqualTo(9) // game2: 7 + 2
        assertThat(result.longestWinStreak).isNotNull
        assertThat(result.longestWinStreak?.teamName).isEqualTo("팀A")
        assertThat(result.longestWinStreak?.streakLength).isEqualTo(2)
    }

    @Test
    fun `should return null for highest scoring game when no completed games`() {
        // given
        val competitionId = 1L
        val league = mockk<League>(relaxed = true)
        val competition =
            Competition(
                league = league,
                name = "2024 춘계대회",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
                status = CompetitionStatus.SCHEDULED,
                id = competitionId,
            )

        every { competitionRepository.findByIdOrNull(competitionId) } returns competition
        every { gameRepository.findByCompetitionId(competitionId) } returns emptyList()
        every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(competitionId) } returns emptyList()

        // when
        val result = service.getReportSummary(competitionId)

        // then
        assertThat(result.totalGames).isEqualTo(0)
        assertThat(result.completedGames).isEqualTo(0)
        assertThat(result.highestScoringGame).isNull()
        assertThat(result.longestWinStreak).isNull()
    }

    @Test
    fun `should calculate longest win streak correctly with loss interruption`() {
        // given
        val competitionId = 1L
        val league = mockk<League>(relaxed = true)
        val competition =
            Competition(
                league = league,
                name = "2024 춘계대회",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
                id = competitionId,
            )
        val team =
            Team(league = league, name = "팀A", city = "서울", foundedYear = 2020, id = 1L)
        val opponentTeam =
            Team(league = league, name = "팀B", city = "부산", foundedYear = 2020, id = 2L)

        val game1 =
            Game.createForTest(
                competition = competition,
                homeTeam = team,
                awayTeam = opponentTeam,
                scheduledAt = LocalDateTime.now().minusDays(5),
                status = GameStatus.FINISHED,
                id = 1L,
            )
        val game2 =
            Game.createForTest(
                competition = competition,
                homeTeam = team,
                awayTeam = opponentTeam,
                scheduledAt = LocalDateTime.now().minusDays(4),
                status = GameStatus.FINISHED,
                id = 2L,
            )
        val game3 =
            Game.createForTest(
                competition = competition,
                homeTeam = team,
                awayTeam = opponentTeam,
                scheduledAt = LocalDateTime.now().minusDays(3),
                status = GameStatus.FINISHED,
                id = 3L,
            )
        val game4 =
            Game.createForTest(
                competition = competition,
                homeTeam = team,
                awayTeam = opponentTeam,
                scheduledAt = LocalDateTime.now().minusDays(2),
                status = GameStatus.FINISHED,
                id = 4L,
            )
        val game5 =
            Game.createForTest(
                competition = competition,
                homeTeam = team,
                awayTeam = opponentTeam,
                scheduledAt = LocalDateTime.now().minusDays(1),
                status = GameStatus.FINISHED,
                id = 5L,
            )

        val gameTeam1 =
            GameTeam(game = game1, team = team, homeAway = HomeAway.HOME, id = 1L).apply {
                updateResult(GameResult.WIN)
            }
        val gameTeam2 =
            GameTeam(game = game2, team = team, homeAway = HomeAway.HOME, id = 2L).apply {
                updateResult(GameResult.WIN)
            }
        val gameTeam3 =
            GameTeam(game = game3, team = team, homeAway = HomeAway.HOME, id = 3L).apply {
                updateResult(GameResult.LOSS)
            }
        val gameTeam4 =
            GameTeam(game = game4, team = team, homeAway = HomeAway.HOME, id = 4L).apply {
                updateResult(GameResult.WIN)
            }
        val gameTeam5 =
            GameTeam(game = game5, team = team, homeAway = HomeAway.HOME, id = 5L).apply {
                updateResult(GameResult.WIN)
            }

        every { competitionRepository.findByIdOrNull(competitionId) } returns competition
        every { gameRepository.findByCompetitionId(competitionId) } returns
            listOf(game1, game2, game3, game4, game5)
        every { gameTeamRepository.findAllByGameIds(any()) } returns emptyList()
        every { battingRecordRepository.findAllByGameId(any()) } returns emptyList()
        every { pitchingRecordRepository.findAllByGameId(any()) } returns emptyList()
        every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(competitionId) } returns
            listOf(gameTeam1, gameTeam2, gameTeam3, gameTeam4, gameTeam5)

        // when
        val result = service.getReportSummary(competitionId)

        // then
        // 2연승 -> 패배 -> 2연승, 최장은 2연승
        assertThat(result.longestWinStreak).isNotNull
        assertThat(result.longestWinStreak?.teamName).isEqualTo("팀A")
        assertThat(result.longestWinStreak?.streakLength).isEqualTo(2)
    }
}
