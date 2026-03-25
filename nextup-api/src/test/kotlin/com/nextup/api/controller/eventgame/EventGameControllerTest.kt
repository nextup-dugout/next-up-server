package com.nextup.api.controller.eventgame

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.domain.eventgame.EventGameParticipantStatus
import com.nextup.core.domain.eventgame.TeamAssignment
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
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

    @Nested
    @DisplayName("POST /api/v1/event-games")
    inner class CreateEventGame {
        @Test
        fun `should create event game`() {
            // given
            val eventGame = createEventGame(1L)
            every { eventGameService.createEventGame(any()) } returns eventGame

            val requestBody =
                mapOf(
                    "title" to "주말 픽업 게임",
                    "scheduledAt" to "2025-05-01T14:00:00",
                    "maxParticipants" to 20,
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/event-games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("주말 픽업 게임"))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/participants/{participantId}/confirm")
    inner class ConfirmParticipant {
        @Test
        fun `should confirm participant`() {
            // given
            val eventGame = createEventGame(1L)
            val participant = createParticipant(10L, eventGame, 50L)
            setParticipantStatus(participant, EventGameParticipantStatus.CONFIRMED)
            every { eventGameService.confirmParticipant(1L, 10L) } returns participant

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/event-games/1/participants/10/confirm"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/event-games/{id}/participants/{participantId}")
    inner class CancelParticipation {
        @Test
        fun `should cancel participation`() {
            // given
            val eventGame = createEventGame(1L)
            val participant = createParticipant(10L, eventGame, 50L)
            setParticipantStatus(participant, EventGameParticipantStatus.CANCELLED)
            every { eventGameService.cancelParticipation(1L, 10L) } returns participant

            // when & then
            mockMvc
                .perform(
                    delete("/api/v1/event-games/1/participants/10"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/close")
    inner class CloseRecruitment {
        @Test
        fun `should close recruitment`() {
            // given
            val eventGame = createEventGame(1L)
            every { eventGameService.closeRecruitment(1L) } returns eventGame

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/event-games/1/close"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/participants/{participantId}/assign-team")
    inner class AssignTeam {
        @Test
        fun `should assign team to participant`() {
            // given
            val eventGame = createEventGame(1L)
            val participant = createParticipant(10L, eventGame, 50L)
            setParticipantStatus(participant, EventGameParticipantStatus.CONFIRMED)
            setParticipantTeam(participant, TeamAssignment.TEAM_A)
            every { eventGameService.assignTeam(1L, 10L, TeamAssignment.TEAM_A) } returns participant

            val requestBody = mapOf("team" to "TEAM_A")

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/event-games/1/participants/10/assign-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.teamAssignment").value("TEAM_A"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/event-games/{id}/auto-assign")
    inner class AutoAssignTeams {
        @Test
        fun `should auto assign teams`() {
            // given
            val eventGame = createEventGame(1L)
            every { eventGameService.autoAssignTeams(1L) } returns eventGame

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/event-games/1/auto-assign"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/complete-assignment")
    inner class CompleteTeamAssignment {
        @Test
        fun `should complete team assignment`() {
            // given
            val eventGame = createEventGame(1L)
            every { eventGameService.completeTeamAssignment(1L) } returns eventGame

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/event-games/1/complete-assignment"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/start")
    inner class StartGame {
        @Test
        fun `should start game`() {
            // given
            val eventGame = createEventGame(1L)
            every { eventGameService.startGame(1L) } returns eventGame

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/event-games/1/start"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/finish")
    inner class FinishGame {
        @Test
        fun `should finish game`() {
            // given
            val eventGame = createEventGame(1L)
            every { eventGameService.finishGame(1L, 5, 3) } returns eventGame

            val requestBody = mapOf("teamAScore" to 5, "teamBScore" to 3)

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/event-games/1/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/cancel")
    inner class CancelGame {
        @Test
        fun `should cancel game`() {
            // given
            val eventGame = createEventGame(1L)
            every { eventGameService.cancelGame(1L, "우천 취소") } returns eventGame

            val requestBody = mapOf("reason" to "우천 취소")

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/event-games/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games/me/history")
    inner class GetMyHistory {
        @Test
        fun `should return my participation history`() {
            // given
            val userId = 100L
            val player = createPlayer(50L)
            val eventGame = createEventGame(1L)
            val participations = listOf(createParticipant(10L, eventGame, 50L))
            every { playerService.getLinkedPlayer(userId) } returns player
            every { eventGameService.getPlayerHistory(50L) } returns participations

            // when & then
            mockMvc
                .perform(get("/api/v1/event-games/me/history"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].playerId").value(50))
        }
    }

    // ──────────────────────────── helpers ────────────────────────────

    private fun createPlayer(id: Long): Player {
        val player = Player(name = "테스트 선수", primaryPosition = Position.CENTER_FIELD)
        val idField = Player::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(player, id)
        return player
    }

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

    private fun setParticipantStatus(
        participant: EventGameParticipant,
        status: EventGameParticipantStatus,
    ) {
        val statusField = EventGameParticipant::class.java.getDeclaredField("status")
        statusField.isAccessible = true
        statusField.set(participant, status)
    }

    private fun setParticipantTeam(
        participant: EventGameParticipant,
        team: TeamAssignment,
    ) {
        val teamField = EventGameParticipant::class.java.getDeclaredField("teamAssignment")
        teamField.isAccessible = true
        teamField.set(participant, team)
    }
}
