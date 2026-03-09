package com.nextup.api.controller

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PitchEvent
import com.nextup.core.domain.game.PitchResult
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.service.PitchEventService
import com.nextup.core.service.PitcherPitchStats
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime

class PitchEventControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var pitchEventService: PitchEventService

    private lateinit var game: Game
    private lateinit var pitcher: GamePlayer
    private lateinit var batter: GamePlayer

    @BeforeEach
    fun setUp() {
        pitchEventService = mockk()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(PitchEventController(pitchEventService))
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        // Test fixtures
        val association = Association(name = "테스트 협회", id = 1L)
        val league =
            League(association = association, name = "테스트 리그", foundedYear = 2024, id = 1L)
        val competition =
            Competition(
                league = league,
                name = "테스트 대회",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
                id = 1L,
            )

        val homeTeam =
            Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        val awayTeam =
            Team(league = league, name = "원정팀", city = "서울", foundedYear = 2020, id = 2L)

        game =
            Game.createForTest(
                competition = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2024, 6, 1, 14, 0),
                status = GameStatus.IN_PROGRESS,
                currentInning = 1,
                id = 1L,
            )

        val homeGameTeam =
            game.gameTeams.first { it.homeAway == HomeAway.HOME }
        val awayGameTeam =
            game.gameTeams.first { it.homeAway == HomeAway.AWAY }

        val pitcherPlayer =
            Player(
                name = "투수",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = Position.STARTING_PITCHER,
                id = 1L,
            )
        pitcher =
            GamePlayer(
                gameTeam = awayGameTeam,
                player = pitcherPlayer,
                position = Position.STARTING_PITCHER,
                battingOrder = null,
                id = 1L,
            )

        val batterPlayer =
            Player(
                name = "타자",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = Position.FIRST_BASE,
                id = 2L,
            )
        batter =
            GamePlayer(
                gameTeam = homeGameTeam,
                player = batterPlayer,
                position = Position.FIRST_BASE,
                battingOrder = 1,
                id = 2L,
            )
    }

    @Test
    fun `경기의 투구 이벤트 목록을 조회할 수 있다`() {
        // given
        val gameId = 1L
        val pitchEvents =
            listOf(
                createPitchEvent(1, PitchResult.STRIKE),
                createPitchEvent(2, PitchResult.BALL),
                createPitchEvent(3, PitchResult.FOUL),
            )

        every { pitchEventService.getGamePitchEvents(gameId) } returns pitchEvents

        // when & then
        mockMvc
            .perform(get("/api/v1/games/{gameId}/pitch-events", gameId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].result").value("STRIKE"))
            .andExpect(jsonPath("$.data[1].result").value("BALL"))
            .andExpect(jsonPath("$.data[2].result").value("FOUL"))
    }

    @Test
    fun `존재하지 않는 경기의 투구 이벤트를 조회하면 404 에러가 발생한다`() {
        // given
        val gameId = 999L
        every { pitchEventService.getGamePitchEvents(gameId) } throws GameNotFoundException(gameId)

        // when & then
        mockMvc
            .perform(get("/api/v1/games/{gameId}/pitch-events", gameId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("GAME_NOT_FOUND"))
    }

    @Test
    fun `경기의 현재 볼카운트를 조회할 수 있다`() {
        // given
        val gameId = 1L
        every { pitchEventService.getCurrentBallCount(gameId) } returns Pair(2, 1)

        // when & then
        mockMvc
            .perform(get("/api/v1/games/{gameId}/ball-count", gameId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.balls").value(2))
            .andExpect(jsonPath("$.data.strikes").value(1))
            .andExpect(jsonPath("$.data.countDisplay").value("2B-1S"))
    }

    @Test
    fun `특정 이닝의 투구 이벤트를 조회할 수 있다`() {
        // given
        val gameId = 1L
        val inning = 3
        val isTopInning = true
        val pitchEvents =
            listOf(
                createPitchEvent(1, PitchResult.STRIKE),
                createPitchEvent(2, PitchResult.BALL),
            )

        every {
            pitchEventService.getInningPitchEvents(gameId, inning, isTopInning)
        } returns pitchEvents

        // when & then
        mockMvc
            .perform(
                get("/api/v1/games/{gameId}/pitch-events/inning/{inning}", gameId, inning)
                    .param("isTopInning", "true"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `투수의 투구 통계를 조회할 수 있다`() {
        // given
        val gamePlayerId = 2L
        val stats =
            PitcherPitchStats(
                totalPitches = 100,
                strikes = 65,
                balls = 20,
                fouls = 10,
                inPlayPitches = 5,
                strikePercentage = 75.0,
                avgPitchesPerAtBat = 4.5,
            )

        every { pitchEventService.calculatePitcherStats(gamePlayerId) } returns stats

        // when & then
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(PitcherStatsController(pitchEventService))
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        mockMvc
            .perform(get("/api/v1/game-players/{gamePlayerId}/pitch-stats", gamePlayerId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalPitches").value(100))
            .andExpect(jsonPath("$.data.strikes").value(65))
            .andExpect(jsonPath("$.data.balls").value(20))
            .andExpect(jsonPath("$.data.strikePercentage").value(75.0))
            .andExpect(jsonPath("$.data.avgPitchesPerAtBat").value(4.5))
    }

    private fun createPitchEvent(
        pitchNumber: Int,
        result: PitchResult,
    ): PitchEvent =
        PitchEvent.create(
            game = game,
            pitcher = pitcher,
            batter = batter,
            pitchNumber = pitchNumber,
            result = result,
            ballCount = 0,
            strikeCount = 1,
        )
}
