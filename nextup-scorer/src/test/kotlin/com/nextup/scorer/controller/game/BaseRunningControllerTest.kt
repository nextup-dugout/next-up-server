package com.nextup.scorer.controller.game

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.BaseRunningResult
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.league.League
import com.nextup.core.service.game.GameScorerService
import com.nextup.scorer.dto.game.BaseRunningRequestDto
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

@DisplayName("GameScorerController - 주루 기록")
class BaseRunningControllerTest {

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
    @DisplayName("POST /api/scorer/games/{gameId}/base-running")
    inner class RecordBaseRunning {

        @Test
        fun `도루 성공을 기록하면 200 OK와 이벤트 정보를 반환한다`() {
            // given
            val gameId = 1L
            val request = BaseRunningRequestDto(
                runnerId = 5L,
                fromBase = Base.FIRST,
                toBase = Base.SECOND,
                result = BaseRunningResult.STOLEN_BASE,
            )
            val game = createGame(gameId, GameStatus.IN_PROGRESS)
            val event = createBaseRunningEvent(game, Base.FIRST, Base.SECOND, BaseRunningResult.STOLEN_BASE)
            every { gameScorerService.recordBaseRunning(gameId, any()) } returns event

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/base-running")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.result").value("STOLEN_BASE"))
                .andExpect(jsonPath("$.data.fromBase").value("FIRST"))
                .andExpect(jsonPath("$.data.toBase").value("SECOND"))

            verify(exactly = 1) { gameScorerService.recordBaseRunning(gameId, any()) }
        }

        @Test
        fun `도루 실패를 기록하면 200 OK와 CAUGHT_STEALING 결과를 반환한다`() {
            // given
            val gameId = 1L
            val request = BaseRunningRequestDto(
                runnerId = 5L,
                fromBase = Base.FIRST,
                toBase = Base.SECOND,
                result = BaseRunningResult.CAUGHT_STEALING,
            )
            val game = createGame(gameId, GameStatus.IN_PROGRESS).apply {
                gameState.outs = 1
            }
            val event = createBaseRunningEvent(game, Base.FIRST, Base.SECOND, BaseRunningResult.CAUGHT_STEALING)
            every { gameScorerService.recordBaseRunning(gameId, any()) } returns event

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/base-running")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.result").value("CAUGHT_STEALING"))

            verify(exactly = 1) { gameScorerService.recordBaseRunning(gameId, any()) }
        }

        @Test
        fun `견제사를 기록하면 200 OK와 PICKED_OFF 결과를 반환한다`() {
            // given
            val gameId = 1L
            val request = BaseRunningRequestDto(
                runnerId = 7L,
                fromBase = Base.SECOND,
                toBase = Base.SECOND,
                result = BaseRunningResult.PICKED_OFF,
            )
            val game = createGame(gameId, GameStatus.IN_PROGRESS)
            val event = createBaseRunningEvent(game, Base.SECOND, Base.SECOND, BaseRunningResult.PICKED_OFF)
            every { gameScorerService.recordBaseRunning(gameId, any()) } returns event

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/base-running")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.result").value("PICKED_OFF"))

            verify(exactly = 1) { gameScorerService.recordBaseRunning(gameId, any()) }
        }

        @Test
        fun `폭투 진루를 기록하면 200 OK와 ADVANCED_ON_WILD_PITCH 결과를 반환한다`() {
            // given
            val gameId = 1L
            val request = BaseRunningRequestDto(
                runnerId = 9L,
                fromBase = Base.SECOND,
                toBase = Base.THIRD,
                result = BaseRunningResult.ADVANCED_ON_WILD_PITCH,
            )
            val game = createGame(gameId, GameStatus.IN_PROGRESS)
            val event = createBaseRunningEvent(game, Base.SECOND, Base.THIRD, BaseRunningResult.ADVANCED_ON_WILD_PITCH)
            every { gameScorerService.recordBaseRunning(gameId, any()) } returns event

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/base-running")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.result").value("ADVANCED_ON_WILD_PITCH"))

            verify(exactly = 1) { gameScorerService.recordBaseRunning(gameId, any()) }
        }

        @Test
        fun `runnerId가 없으면 400 Bad Request를 반환한다`() {
            // given
            val gameId = 1L
            val request = BaseRunningRequestDto(
                runnerId = null,
                fromBase = Base.FIRST,
                toBase = Base.SECOND,
                result = BaseRunningResult.STOLEN_BASE,
            )

            // when & then
            mockMvc.perform(
                post("/api/scorer/games/$gameId/base-running")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }
    }

    private fun createAssociation(id: Long, name: String): Association {
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

    private fun createLeague(id: Long, name: String, association: Association): League {
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

    private fun createCompetition(id: Long, name: String, league: League): Competition {
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

    private fun createGame(id: Long, status: GameStatus): Game {
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
            currentInning = 3,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState()
        ).apply {
            val idField = Game::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createBaseRunningEvent(
        game: Game,
        fromBase: Base,
        toBase: Base,
        result: BaseRunningResult,
    ): GameEvent {
        val runner = mockk<com.nextup.core.domain.game.GamePlayer>(relaxed = true)
        return GameEvent.createBaseRunning(
            game = game,
            runner = runner,
            fromBase = fromBase,
            toBase = toBase,
            result = result,
            description = "${result.displayName}: $fromBase → $toBase",
            outCountBefore = game.gameState.outs,
            outCountAfter = game.gameState.outs,
            runnersBeforeJson = null,
            runnersAfterJson = null,
        )
    }
}
