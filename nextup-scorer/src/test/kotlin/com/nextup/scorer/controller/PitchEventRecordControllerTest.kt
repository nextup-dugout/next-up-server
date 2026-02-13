package com.nextup.scorer.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.*
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.service.PitchEventService
import com.nextup.scorer.dto.pitch.RecordPitchRequest
import com.nextup.scorer.exception.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime

class PitchEventRecordControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var pitchEventService: PitchEventService
    private lateinit var objectMapper: ObjectMapper

    private lateinit var game: Game
    private lateinit var pitcher: GamePlayer
    private lateinit var batter: GamePlayer

    @BeforeEach
    fun setUp() {
        pitchEventService = mockk()
        objectMapper = ObjectMapper()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(PitchEventRecordController(pitchEventService))
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        // Test fixtures
        game = createGame()
        pitcher = createPitcher(game)
        batter = createBatter(game)
    }

    @Test
    fun `투구를 기록할 수 있다`() {
        // given
        val gameId = 1L
        val request =
            RecordPitchRequest(
                pitcherId = 2L,
                batterId = 3L,
                result = PitchResult.STRIKE,
                description = "스트라이크",
            )

        val pitchEvent = createPitchEvent()

        every {
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = request.pitcherId,
                batterId = request.batterId,
                result = request.result,
                description = request.description,
            )
        } returns pitchEvent

        // when & then
        mockMvc
            .perform(
                post("/scorer/v1/games/{gameId}/pitch-events", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.result").value("STRIKE"))
            .andExpect(jsonPath("$.data.ballCount").value(0))
            .andExpect(jsonPath("$.data.strikeCount").value(1))
            .andExpect(jsonPath("$.data.countDisplay").value("0B-1S"))
    }

    @Test
    fun `투수 ID가 없으면 400 에러가 발생한다`() {
        // given
        val gameId = 1L
        val invalidRequest = """{"batterId": 3, "result": "STRIKE"}"""

        // when & then
        mockMvc
            .perform(
                post("/scorer/v1/games/{gameId}/pitch-events", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `타자 ID가 없으면 400 에러가 발생한다`() {
        // given
        val gameId = 1L
        val invalidRequest = """{"pitcherId": 2, "result": "STRIKE"}"""

        // when & then
        mockMvc
            .perform(
                post("/scorer/v1/games/{gameId}/pitch-events", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `투구 결과가 없으면 400 에러가 발생한다`() {
        // given
        val gameId = 1L
        val invalidRequest = """{"pitcherId": 2, "batterId": 3}"""

        // when & then
        mockMvc
            .perform(
                post("/scorer/v1/games/{gameId}/pitch-events", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest),
            ).andExpect(status().isBadRequest)
    }

    // Helper methods
    private fun createGame(): Game {
        val association = Association(name = "테스트 협회", id = 1L)
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020, id = 1L)
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

        return Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2024, 6, 1, 14, 0),
            status = GameStatus.IN_PROGRESS,
            currentInning = 1,
            id = 1L,
        )
    }

    private fun createHomeTeam(game: Game): GameTeam {
        val association = Association(name = "테스트 협회", id = 1L)
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020, id = 1L)
        val team = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        return GameTeam(game = game, team = team, homeAway = HomeAway.HOME, id = 1L)
    }

    private fun createAwayTeam(game: Game): GameTeam {
        val association = Association(name = "테스트 협회", id = 2L)
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020, id = 2L)
        val team = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
        return GameTeam(game = game, team = team, homeAway = HomeAway.AWAY, id = 2L)
    }

    private fun createPitcher(game: Game): GamePlayer {
        val awayTeam = createAwayTeam(game)
        val player =
            Player(
                name = "투수",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = Position.STARTING_PITCHER,
            )
        return GamePlayer(
            gameTeam = awayTeam,
            player = player,
            position = Position.STARTING_PITCHER,
            battingOrder = null,
        )
    }

    private fun createBatter(game: Game): GamePlayer {
        val homeTeam = createHomeTeam(game)
        val player =
            Player(
                name = "타자",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = Position.FIRST_BASE,
            )
        return GamePlayer(
            gameTeam = homeTeam,
            player = player,
            position = Position.FIRST_BASE,
            battingOrder = 1,
        )
    }

    private fun createPitchEvent(): PitchEvent =
        PitchEvent.create(
            game = game,
            pitcher = pitcher,
            batter = batter,
            pitchNumber = 1,
            result = PitchResult.STRIKE,
            ballCount = 0,
            strikeCount = 1,
            description = "스트라이크",
        )
}
