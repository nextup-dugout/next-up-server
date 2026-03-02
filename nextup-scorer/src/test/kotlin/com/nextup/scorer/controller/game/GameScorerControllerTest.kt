package com.nextup.scorer.controller.game

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Position
import com.nextup.core.service.game.GameScorerService
import com.nextup.core.service.game.dto.GameEndReason
import com.nextup.scorer.dto.game.GameEndRequestDto
import com.nextup.scorer.dto.game.PlateAppearanceRequestDto
import com.nextup.scorer.dto.game.RunnerMovementDto
import com.nextup.scorer.dto.game.SubstitutionRequestDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameScorerController")
class GameScorerControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var gameScorerService: GameScorerService
    private lateinit var controller: GameScorerController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        gameScorerService = mockk()
        controller = GameScorerController(gameScorerService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/start")
    inner class StartGame {

        @Test
        fun `should start game successfully`() {
            // given
            val gameId = 1L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = true
                }
            every { gameScorerService.startGame(gameId) } returns game

            // when & then
            mockMvc.perform(post("/api/scorer/games/$gameId/start"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentInning").value(1))
                .andExpect(jsonPath("$.data.isTopInning").value(true))

            verify(exactly = 1) { gameScorerService.startGame(gameId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/plate-appearances")
    inner class RecordPlateAppearance {

        @Test
        fun `should record plate appearance with single hit`() {
            // given
            val gameId = 1L
            val request =
                PlateAppearanceRequestDto(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 2,
                    strikes = 1
                )
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 3
                    isTopInning = false
                    gameState.runnerOnFirstId = 10L
                }
            every { gameScorerService.recordPlateAppearance(gameId, any()) } returns game

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/plate-appearances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentInning").value(3))

            verify(exactly = 1) { gameScorerService.recordPlateAppearance(gameId, any()) }
        }

        @Test
        fun `should record plate appearance with runner movements`() {
            // given
            val gameId = 1L
            val request =
                PlateAppearanceRequestDto(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.DOUBLE,
                    runnerMovements =
                        listOf(
                            RunnerMovementDto(
                                runnerId = 5L,
                                fromBase = Base.FIRST,
                                toBase = Base.THIRD,
                                isOut = false
                            )
                        ),
                    rbis = 1,
                    balls = 0,
                    strikes = 2
                )
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                    gameState.runnerOnSecondId = 10L
                    gameState.runnerOnThirdId = 5L
                }
            every { gameScorerService.recordPlateAppearance(gameId, any()) } returns game

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/plate-appearances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.gameState.runnerOnSecondId").value(10))
                .andExpect(jsonPath("$.data.gameState.runnerOnThirdId").value(5))

            verify(exactly = 1) { gameScorerService.recordPlateAppearance(gameId, any()) }
        }

        @Test
        fun `should record strikeout`() {
            // given
            val gameId = 1L
            val request =
                PlateAppearanceRequestDto(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 1,
                    strikes = 3
                )
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 2
                    isTopInning = true
                    gameState.outs = 1
                }
            every { gameScorerService.recordPlateAppearance(gameId, any()) } returns game

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/plate-appearances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameState.outs").value(1))

            verify(exactly = 1) { gameScorerService.recordPlateAppearance(gameId, any()) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/half-inning")
    inner class AdvanceHalfInning {

        @Test
        fun `should advance to next half inning`() {
            // given
            val gameId = 1L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = false // 1회말로 진행
                    gameState.resetForNewInning()
                }
            every { gameScorerService.advanceHalfInning(gameId) } returns game

            // when & then
            mockMvc.perform(post("/api/scorer/games/$gameId/half-inning"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.currentInning").value(1))
                .andExpect(jsonPath("$.data.isTopInning").value(false))
                .andExpect(jsonPath("$.data.gameState.outs").value(0))

            verify(exactly = 1) { gameScorerService.advanceHalfInning(gameId) }
        }

        @Test
        fun `should advance from bottom to next top inning`() {
            // given
            val gameId = 1L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 2
                    isTopInning = true // 2회초로 진행
                    gameState.resetForNewInning()
                }
            every { gameScorerService.advanceHalfInning(gameId) } returns game

            // when & then
            mockMvc.perform(post("/api/scorer/games/$gameId/half-inning"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentInning").value(2))
                .andExpect(jsonPath("$.data.isTopInning").value(true))

            verify(exactly = 1) { gameScorerService.advanceHalfInning(gameId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/end")
    inner class EndGame {

        @Test
        fun `should end game with regulation finish`() {
            // given
            val gameId = 1L
            val request = GameEndRequestDto(reason = GameEndReason.REGULATION)
            val game =
                createGame(gameId, GameStatus.FINISHED).apply {
                    currentInning = 9
                    isTopInning = false
                }
            every { gameScorerService.endGame(gameId, GameEndReason.REGULATION) } returns game

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/end")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.status").value("FINISHED"))

            verify(exactly = 1) { gameScorerService.endGame(gameId, GameEndReason.REGULATION) }
        }

        @Test
        fun `should end game with mercy rule`() {
            // given
            val gameId = 1L
            val request = GameEndRequestDto(reason = GameEndReason.MERCY_RULE)
            val game =
                createGame(gameId, GameStatus.CALLED).apply {
                    currentInning = 7
                    isTopInning = true
                }
            every { gameScorerService.endGame(gameId, GameEndReason.MERCY_RULE) } returns game

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/end")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CALLED"))

            verify(exactly = 1) { gameScorerService.endGame(gameId, GameEndReason.MERCY_RULE) }
        }

        @Test
        fun `should end game due to weather`() {
            // given
            val gameId = 1L
            val request = GameEndRequestDto(reason = GameEndReason.WEATHER)
            val game =
                createGame(gameId, GameStatus.CALLED).apply {
                    currentInning = 5
                    isTopInning = false
                }
            every { gameScorerService.endGame(gameId, GameEndReason.WEATHER) } returns game

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/end")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CALLED"))

            verify(exactly = 1) { gameScorerService.endGame(gameId, GameEndReason.WEATHER) }
        }

        @Test
        fun `should end game with forfeit`() {
            // given
            val gameId = 1L
            val request = GameEndRequestDto(reason = GameEndReason.FORFEIT)
            val game =
                createGame(gameId, GameStatus.FORFEITED).apply {
                    currentInning = 3
                    isTopInning = true
                }
            every { gameScorerService.endGame(gameId, GameEndReason.FORFEIT) } returns game

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/end")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FORFEITED"))

            verify(exactly = 1) { gameScorerService.endGame(gameId, GameEndReason.FORFEIT) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/substitutions")
    inner class SubstitutePlayer {

        @Test
        fun `선수 교체 요청이 성공적으로 처리된다`() {
            // given
            val gameId = 1L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }
            val incomingPlayer = mockk<GamePlayer>(relaxed = true)
            val outgoingPlayer = mockk<GamePlayer>(relaxed = true)
            every { incomingPlayer.id } returns 20L
            every { outgoingPlayer.id } returns 10L

            val substitutionEvent =
                GameEvent(
                    game = game,
                    inning = 5,
                    isTopInning = true,
                    outCountBefore = 1,
                    outCountAfter = 1,
                    eventType = GameEventType.SUBSTITUTION,
                    description = "5회초: 홍길동 → 김철수 (좌익수)",
                    batter = incomingPlayer,
                    pitcher = outgoingPlayer,
                ).apply {
                    val idField = GameEvent::class.java.getDeclaredField("id")
                    idField.isAccessible = true
                    idField.set(this, 100L)
                }

            every { gameScorerService.substitutePlayer(gameId, any()) } returns substitutionEvent

            val request =
                SubstitutionRequestDto(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/substitutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(100))
                .andExpect(jsonPath("$.data.inning").value(5))
                .andExpect(jsonPath("$.data.isTopInning").value(true))
                .andExpect(jsonPath("$.data.description").value("5회초: 홍길동 → 김철수 (좌익수)"))

            verify(exactly = 1) { gameScorerService.substitutePlayer(gameId, any()) }
        }
    }

    private fun createAssociation(
        id: Long,
        name: String
    ): Association {
        return Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association
    ): League {
        return League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createCompetition(
        id: Long,
        name: String,
        league: League
    ): Competition {
        return Competition(
            league = league,
            name = name,
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            endDate = LocalDate.of(2025, 6, 30),
            status = CompetitionStatus.IN_PROGRESS,
            description = null,
            maxTeams = null
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createGame(
        id: Long,
        status: GameStatus
    ): Game {
        val association = createAssociation(1L, "서울시야구협회")
        val league = createLeague(1L, "1부 리그", association)
        val competition = createCompetition(1L, "2025 춘계대회", league)

        return Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실구장",
            fieldName = "1구장",
            gameNumber = 1,
            status = status,
            currentInning = 1,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState()
        ).apply {
            val idField = Game::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
