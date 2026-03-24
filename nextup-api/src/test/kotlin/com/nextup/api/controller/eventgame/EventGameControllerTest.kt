package com.nextup.api.controller.eventgame

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.service.eventgame.EventGameService
import com.nextup.core.service.player.PlayerService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@DisplayName("EventGameController")
class EventGameControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var eventGameService: EventGameService
    private lateinit var playerService: PlayerService
    private lateinit var controller: EventGameController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        eventGameService = mockk()
        playerService = mockk()
        controller = EventGameController(eventGameService, playerService)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(100L, null, emptyList())
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .build()
        objectMapper = ObjectMapper().findAndRegisterModules()
    }

    @Nested
    @DisplayName("POST /api/v1/event-games/{id}/join")
    inner class JoinEventGame {
        @Test
        fun `should join event game with userId resolution`() {
            // given
            val userId = 100L
            val eventGame = createEventGame(1L)
            val participant = createParticipant(10L, eventGame, 50L)
            every {
                eventGameService.joinEventGame(
                    eventGameId = 1L,
                    userId = userId,
                    message = "참가합니다",
                )
            } returns participant

            val requestBody = mapOf("message" to "참가합니다")

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/event-games/1/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.playerId").value(50))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.message").value("참가합니다"))

            verify(exactly = 1) {
                eventGameService.joinEventGame(
                    eventGameId = 1L,
                    userId = userId,
                    message = "참가합니다",
                )
            }
        }

        @Test
        fun `should join event game without message`() {
            // given
            val userId = 100L
            val eventGame = createEventGame(1L)
            val participant = createParticipant(11L, eventGame, 50L)
            every {
                eventGameService.joinEventGame(
                    eventGameId = 1L,
                    userId = userId,
                    message = null,
                )
            } returns participant

            val requestBody = emptyMap<String, Any>()

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/event-games/1/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(11))

            verify(exactly = 1) {
                eventGameService.joinEventGame(
                    eventGameId = 1L,
                    userId = userId,
                    message = null,
                )
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games")
    inner class GetRecruitingEventGames {
        @Test
        fun `should return recruiting event games`() {
            // given
            val games = listOf(createEventGame(1L), createEventGame(2L))
            every { eventGameService.getRecruitingEventGames() } returns games

            // when & then
            mockMvc
                .perform(get("/api/v1/event-games"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games/{id}")
    inner class GetEventGame {
        @Test
        fun `should return event game detail`() {
            // given
            val eventGame = createEventGame(1L)
            every { eventGameService.getEventGame(1L) } returns eventGame

            // when & then
            mockMvc
                .perform(get("/api/v1/event-games/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("주말 픽업 게임"))
                .andExpect(jsonPath("$.data.status").value("RECRUITING"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games/me")
    inner class GetMyEventGames {
        @Test
        fun `should return my event games`() {
            // given
            val userId = 100L
            val games = listOf(createEventGame(1L))
            every { eventGameService.getMyEventGames(userId) } returns games

            // when & then
            mockMvc
                .perform(get("/api/v1/event-games/me"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games/{id}/participants")
    inner class GetParticipants {
        @Test
        fun `should return participants list`() {
            // given
            val eventGame = createEventGame(1L)
            val participants =
                listOf(
                    createParticipant(10L, eventGame, 50L),
                    createParticipant(11L, eventGame, 51L),
                )
            every { eventGameService.getParticipants(1L) } returns participants

            // when & then
            mockMvc
                .perform(get("/api/v1/event-games/1/participants"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
        }
    }

    // ──────────────────────────── helpers ────────────────────────────

    private fun createEventGame(id: Long): EventGame {
        val game =
            EventGame.create(
                organizerId = 100L,
                title = "주말 픽업 게임",
                scheduledAt = LocalDateTime.of(2025, 5, 1, 14, 0),
                maxParticipants = 20,
            )
        val idField = EventGame::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(game, id)
        return game
    }

    private fun createParticipant(
        id: Long,
        eventGame: EventGame,
        playerId: Long,
    ): EventGameParticipant {
        val participant =
            EventGameParticipant.create(
                eventGame = eventGame,
                playerId = playerId,
                message = "참가합니다",
            )
        val idField = EventGameParticipant::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(participant, id)
        return participant
    }
}
