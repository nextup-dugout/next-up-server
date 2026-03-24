package com.nextup.api.controller.attendance

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.api.dto.attendance.NudgeRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.service.attendance.NudgeResult
import com.nextup.core.service.attendance.NudgeService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AttendanceControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var nudgeService: NudgeService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        nudgeService = mockk()
        objectMapper = ObjectMapper()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(NudgeController(nudgeService))
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                    object : org.springframework.web.method.support.HandlerMethodArgumentResolver {
                        override fun supportsParameter(parameter: org.springframework.core.MethodParameter): Boolean =
                            parameter.hasParameterAnnotation(
                                org.springframework.security.core.annotation.AuthenticationPrincipal::class.java,
                            )

                        override fun resolveArgument(
                            parameter: org.springframework.core.MethodParameter,
                            mavContainer: org.springframework.web.method.support.ModelAndViewContainer?,
                            webRequest: org.springframework.web.context.request.NativeWebRequest,
                            binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
                        ): Any = 1L
                    },
                ).build()
    }

    @Test
    fun `should return nudge result when non-voters exist`() {
        // given
        val gameId = 1L
        val result =
            NudgeResult(
                notifiedCount = 3,
                nonVoterNames = listOf("Player1", "Player2", "Player3"),
            )

        every { nudgeService.nudgeNonVoters(gameId, null) } returns result

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/{gameId}/attendance/nudge", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.notifiedCount").value(3))
            .andExpect(jsonPath("$.data.nonVoterNames[0]").value("Player1"))
            .andExpect(jsonPath("$.data.nonVoterNames[1]").value("Player2"))
            .andExpect(jsonPath("$.data.nonVoterNames[2]").value("Player3"))

        verify(exactly = 1) { nudgeService.nudgeNonVoters(gameId, null) }
    }

    @Test
    fun `should return empty result when no non-voters exist`() {
        // given
        val gameId = 1L
        val result =
            NudgeResult(
                notifiedCount = 0,
                nonVoterNames = emptyList(),
            )

        every { nudgeService.nudgeNonVoters(gameId, null) } returns result

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/{gameId}/attendance/nudge", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.notifiedCount").value(0))
            .andExpect(jsonPath("$.data.nonVoterNames").isEmpty)

        verify(exactly = 1) { nudgeService.nudgeNonVoters(gameId, null) }
    }

    @Test
    fun `should send custom message when provided`() {
        // given
        val gameId = 1L
        val customMessage = "Please vote ASAP!"
        val request = NudgeRequest(message = customMessage)
        val result =
            NudgeResult(
                notifiedCount = 2,
                nonVoterNames = listOf("Player1", "Player2"),
            )

        every { nudgeService.nudgeNonVoters(gameId, customMessage) } returns result

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/{gameId}/attendance/nudge", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.notifiedCount").value(2))

        verify(exactly = 1) { nudgeService.nudgeNonVoters(gameId, customMessage) }
    }

    @Test
    fun `should handle empty request body with default values`() {
        // given
        val gameId = 1L
        val result =
            NudgeResult(
                notifiedCount = 1,
                nonVoterNames = listOf("Player1"),
            )

        every { nudgeService.nudgeNonVoters(gameId, null) } returns result

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/{gameId}/attendance/nudge", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.notifiedCount").value(1))
            .andExpect(jsonPath("$.data.nonVoterNames[0]").value("Player1"))

        verify(exactly = 1) { nudgeService.nudgeNonVoters(gameId, null) }
    }

    @Test
    fun `should return 404 when game does not exist`() {
        // given
        val gameId = 999L

        every { nudgeService.nudgeNonVoters(gameId, null) } throws GameNotFoundException(gameId)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/{gameId}/attendance/nudge", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("GAME_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").value("Game not found: $gameId"))

        verify(exactly = 1) { nudgeService.nudgeNonVoters(gameId, null) }
    }

    @Test
    fun `should handle request without body`() {
        // given
        val gameId = 1L
        val result =
            NudgeResult(
                notifiedCount = 1,
                nonVoterNames = listOf("Player1"),
            )

        every { nudgeService.nudgeNonVoters(gameId, null) } returns result

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/{gameId}/attendance/nudge", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(exactly = 1) { nudgeService.nudgeNonVoters(gameId, null) }
    }

    @Test
    fun `should handle multiple non-voters with various names`() {
        // given
        val gameId = 1L
        val result =
            NudgeResult(
                notifiedCount = 5,
                nonVoterNames = listOf("김철수", "이영희", "박민수", "최지원", "정다은"),
            )

        every { nudgeService.nudgeNonVoters(gameId, null) } returns result

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/{gameId}/attendance/nudge", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.notifiedCount").value(5))
            .andExpect(jsonPath("$.data.nonVoterNames.length()").value(5))
            .andExpect(jsonPath("$.data.nonVoterNames[0]").value("김철수"))
            .andExpect(jsonPath("$.data.nonVoterNames[4]").value("정다은"))

        verify(exactly = 1) { nudgeService.nudgeNonVoters(gameId, null) }
    }
}
