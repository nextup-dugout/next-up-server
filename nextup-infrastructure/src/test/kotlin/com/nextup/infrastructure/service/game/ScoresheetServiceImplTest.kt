package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.*
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.*
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ScoresheetServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var scoresheetService: ScoresheetServiceImpl

    private lateinit var game: Game
    private lateinit var homeTeam: Team
    private lateinit var awayTeam: Team
    private lateinit var homeGameTeam: GameTeam
    private lateinit var awayGameTeam: GameTeam

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gameTeamRepository = mockk()
        gamePlayerRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        gameEventRepository = mockk()

        scoresheetService =
            ScoresheetServiceImpl(
                gameRepository,
                gameTeamRepository,
                gamePlayerRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                gameEventRepository,
            )

        val association = Association(name = "테스트협회", id = 1L)
        val league =
            League(
                association = association,
                name = "테스트리그",
                foundedYear = 2020,
                id = 1L,
            )
        val competition =
            Competition(
                league = league,
                name = "2024 시즌",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
                id = 1L,
            )

        homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)

        game =
            Game(
                competition = competition,
                scheduledAt = LocalDateTime.of(2024, 3, 15, 14, 0),
                location = "서울",
                fieldName = "잠실구장",
                gameNumber = 1,
                status = GameStatus.FINISHED,
                currentInning = 9,
                isTopInning = false,
                totalInnings = 9,
                startedAt = LocalDateTime.of(2024, 3, 15, 14, 10),
                endedAt = LocalDateTime.of(2024, 3, 15, 17, 0),
                id = 100L,
            )

        homeGameTeam =
            GameTeam(
                game = game,
                team = homeTeam,
                homeAway = HomeAway.HOME,
                id = 1L,
            )
        homeGameTeam.updateScore(totalScore = 5, totalHits = 10, totalErrors = 1)
        homeGameTeam.updateResult(GameResult.WIN)
        for (i in 1..9) {
            homeGameTeam.recordInningScore(i, listOf(0, 2, 0, 1, 0, 0, 2, 0, 0)[i - 1])
        }

        awayGameTeam =
            GameTeam(
                game = game,
                team = awayTeam,
                homeAway = HomeAway.AWAY,
                id = 2L,
            )
        awayGameTeam.updateScore(totalScore = 3, totalHits = 8, totalErrors = 2)
        awayGameTeam.updateResult(GameResult.LOSS)
        for (i in 1..9) {
            awayGameTeam.recordInningScore(i, listOf(1, 0, 2, 0, 0, 0, 0, 0, 0)[i - 1])
        }
    }

    @Test
    fun `should get scoresheet successfully`() {
        // given
        val homePlayer = Player(name = "홈타자1", primaryPosition = Position.FIRST_BASE, id = 1L)
        val awayPlayer = Player(name = "원정타자1", primaryPosition = Position.SHORTSTOP, id = 2L)

        val homeGamePlayer =
            GamePlayer.createStarter(
                gameTeam = homeGameTeam,
                player = homePlayer,
                position = Position.FIRST_BASE,
                battingOrder = 1,
                backNumber = 10,
            )

        val awayGamePlayer =
            GamePlayer.createStarter(
                gameTeam = awayGameTeam,
                player = awayPlayer,
                position = Position.SHORTSTOP,
                battingOrder = 1,
                backNumber = 5,
            )

        val homeBattingRecord =
            mockk<BattingRecord> {
                every { plateAppearances } returns 4
                every { atBats } returns 3
                every { runs } returns 2
                every { hits } returns 2
                every { doubles } returns 1
                every { triples } returns 0
                every { homeRuns } returns 0
                every { runsBattedIn } returns 1
                every { walks } returns 1
                every { strikeouts } returns 1
                every { stolenBases } returns 0
                every { battingAverage } returns BigDecimal("0.667")
            }

        val awayBattingRecord =
            mockk<BattingRecord> {
                every { plateAppearances } returns 4
                every { atBats } returns 4
                every { runs } returns 1
                every { hits } returns 1
                every { doubles } returns 0
                every { triples } returns 0
                every { homeRuns } returns 0
                every { runsBattedIn } returns 0
                every { walks } returns 0
                every { strikeouts } returns 2
                every { stolenBases } returns 1
                every { battingAverage } returns BigDecimal("0.250")
            }

        every { gameRepository.findByIdOrNull(100L) } returns game
        every { gameTeamRepository.findAllByGameId(100L) } returns listOf(homeGameTeam, awayGameTeam)
        every { gamePlayerRepository.findAllByGameId(100L) } returns listOf(homeGamePlayer, awayGamePlayer)
        every { battingRecordRepository.findByGamePlayer(homeGamePlayer) } returns homeBattingRecord
        every { battingRecordRepository.findByGamePlayer(awayGamePlayer) } returns awayBattingRecord
        every { pitchingRecordRepository.findByGamePlayer(any()) } returns null
        every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(100L) } returns emptyList()

        // when
        val result = scoresheetService.getScoresheet(100L)

        // then
        assertThat(result.gameInfo.gameId).isEqualTo(100L)
        assertThat(result.gameInfo.competitionName).isEqualTo("2024 시즌")
        assertThat(result.teams.home.teamName).isEqualTo("홈팀")
        assertThat(result.teams.away.teamName).isEqualTo("원정팀")
        assertThat(result.teams.home.totalScore).isEqualTo(5)
        assertThat(result.teams.away.totalScore).isEqualTo(3)
        assertThat(result.inningScores.innings).isEqualTo(9)
        assertThat(result.battingRecords.home).hasSize(1)
        assertThat(result.battingRecords.away).hasSize(1)
        assertThat(result.battingRecords.home[0].name).isEqualTo("홈타자1")
        assertThat(result.battingRecords.home[0].hits).isEqualTo(2)
    }

    @Test
    fun `should throw GameNotFoundException when game not found`() {
        // given
        every { gameRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows(GameNotFoundException::class.java) {
            scoresheetService.getScoresheet(999L)
        }
    }

    @Test
    fun `should throw InvalidStateException when game teams size is not 2`() {
        // given
        every { gameRepository.findByIdOrNull(100L) } returns game
        every { gameTeamRepository.findAllByGameId(100L) } returns listOf(homeGameTeam)

        // when & then
        val exception =
            assertThrows(InvalidStateException::class.java) {
                scoresheetService.getScoresheet(100L)
            }
        assertThat(exception.code).isEqualTo("INVALID_GAME_TEAMS")
    }

    @Test
    fun `should throw InvalidStateException when home team is missing`() {
        // given
        val anotherAwayTeam =
            GameTeam(
                game = game,
                team = awayTeam,
                homeAway = HomeAway.AWAY,
                id = 3L,
            )

        every { gameRepository.findByIdOrNull(100L) } returns game
        every { gameTeamRepository.findAllByGameId(100L) } returns listOf(awayGameTeam, anotherAwayTeam)

        // when & then
        val exception =
            assertThrows(InvalidStateException::class.java) {
                scoresheetService.getScoresheet(100L)
            }
        assertThat(exception.code).isEqualTo("MISSING_HOME_TEAM")
    }

    @Test
    fun `should display infinity symbol for null ERA pitcher`() {
        // given
        val pitcher = Player(name = "투수1", primaryPosition = Position.STARTING_PITCHER, id = 10L)

        val pitcherGamePlayer =
            GamePlayer.createStarter(
                gameTeam = homeGameTeam,
                player = pitcher,
                position = Position.STARTING_PITCHER,
                battingOrder = null,
                backNumber = 1,
            )

        val pitchingRecord =
            mockk<PitchingRecord> {
                every { isStartingPitcher } returns true
                every { inningsPitchedDisplay } returns "0.0"
                every { hitsAllowed } returns 2
                every { runsAllowed } returns 3
                every { earnedRuns } returns 3
                every { walksAllowed } returns 1
                every { strikeouts } returns 0
                every { homeRunsAllowed } returns 0
                every { decision } returns PitchingDecision.LOSS
                every { earnedRunAverage } returns null
            }

        every { gameRepository.findByIdOrNull(100L) } returns game
        every { gameTeamRepository.findAllByGameId(100L) } returns listOf(homeGameTeam, awayGameTeam)
        every { gamePlayerRepository.findAllByGameId(100L) } returns listOf(pitcherGamePlayer)
        every { battingRecordRepository.findByGamePlayer(any()) } returns null
        every { pitchingRecordRepository.findByGamePlayer(pitcherGamePlayer) } returns pitchingRecord
        every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(100L) } returns emptyList()

        // when
        val result = scoresheetService.getScoresheet(100L)

        // then
        assertThat(result.pitchingRecords.home).hasSize(1)
        assertThat(result.pitchingRecords.home[0].era).isEqualTo("\u221E")
    }

    @Test
    fun `should handle players without batting records`() {
        // given
        val player = Player(name = "선수1", primaryPosition = Position.STARTING_PITCHER, id = 1L)

        val gamePlayer =
            GamePlayer.createStarter(
                gameTeam = homeGameTeam,
                player = player,
                position = Position.STARTING_PITCHER,
                battingOrder = null,
                backNumber = 1,
            )

        every { gameRepository.findByIdOrNull(100L) } returns game
        every { gameTeamRepository.findAllByGameId(100L) } returns listOf(homeGameTeam, awayGameTeam)
        every { gamePlayerRepository.findAllByGameId(100L) } returns listOf(gamePlayer)
        every { battingRecordRepository.findByGamePlayer(any()) } returns null
        every { pitchingRecordRepository.findByGamePlayer(any()) } returns null
        every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(100L) } returns emptyList()

        // when
        val result = scoresheetService.getScoresheet(100L)

        // then
        assertThat(result.battingRecords.home).isEmpty()
        assertThat(result.battingRecords.away).isEmpty()
    }
}
