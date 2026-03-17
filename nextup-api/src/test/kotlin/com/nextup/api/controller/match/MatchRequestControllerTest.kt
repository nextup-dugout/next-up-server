package com.nextup.api.controller.match

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.match.CreateMatchRequestApiRequest
import com.nextup.api.dto.match.RespondToMatchRequestApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.MatchRequestNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import com.nextup.core.domain.match.MatchResponse
import com.nextup.core.domain.match.MatchResponseStatus
import com.nextup.core.domain.match.SkillLevel
import com.nextup.core.domain.team.Team
import com.nextup.core.service.match.MatchingService
import com.nextup.core.service.match.dto.MatchRequestFilterDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.LocalDate

@DisplayName("MatchRequestController")
class MatchRequestControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var matchingService: MatchingService
    private lateinit var objectMapper: ObjectMapper

    private val mockTeam = mockk<Team>(relaxed = true)
    private val mockRespondTeam = mockk<Team>(relaxed = true)
    private val mockMatchRequest = mockk<MatchRequest>(relaxed = true)
    private val mockMatchResponse = mockk<MatchResponse>(relaxed = true)

    @BeforeEach
    fun setUp() {
        matchingService = mockk()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        val controller = MatchRequestController(matchingService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        every { mockTeam.id } returns 1L
        every { mockTeam.name } returns "테스트 팀"
        every { mockRespondTeam.id } returns 2L
        every { mockRespondTeam.name } returns "응답 팀"

        every { mockMatchRequest.id } returns 1L
        every { mockMatchRequest.team } returns mockTeam
        every { mockMatchRequest.preferredDate } returns LocalDate.now().plusDays(7)
        every { mockMatchRequest.preferredTime } returns "14:00-17:00"
        every { mockMatchRequest.preferredLocation } returns "서울 야구장"
        every { mockMatchRequest.message } returns "연습 경기 희망합니다"
        every { mockMatchRequest.skillLevel } returns SkillLevel.INTERMEDIATE
        every { mockMatchRequest.status } returns MatchRequestStatus.OPEN
        every { mockMatchRequest.createdAt } returns Instant.now()

        every { mockMatchResponse.id } returns 1L
        every { mockMatchResponse.matchRequest } returns mockMatchRequest
        every { mockMatchResponse.respondTeam } returns mockRespondTeam
        every { mockMatchResponse.message } returns "함께 경기하고 싶습니다"
        every { mockMatchResponse.status } returns MatchResponseStatus.PENDING
        every { mockMatchResponse.createdAt } returns Instant.now()
    }

    @Test
    fun `should create match request successfully`() {
        // given
        val request =
            CreateMatchRequestApiRequest(
                teamId = 1L,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = "14:00-17:00",
                preferredLocation = "서울 야구장",
                message = "연습 경기 희망합니다",
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        every { matchingService.createRequest(any()) } returns mockMatchRequest

        // when & then
        mockMvc
            .perform(
                post("/api/v1/match-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.teamId").value(1))
            .andExpect(jsonPath("$.data.teamName").value("테스트 팀"))
            .andExpect(jsonPath("$.data.skillLevel").value("INTERMEDIATE"))
            .andExpect(jsonPath("$.data.status").value("OPEN"))

        verify(exactly = 1) { matchingService.createRequest(any()) }
    }

    @Test
    fun `should fail to create match request with invalid data`() {
        // given
        val invalidRequest =
            mapOf(
                "teamId" to null,
                "preferredDate" to null,
                "skillLevel" to null,
            )

        // when & then
        mockMvc
            .perform(
                post("/api/v1/match-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))

        verify(exactly = 0) { matchingService.createRequest(any()) }
    }

    @Test
    fun `should fail to create match request when team not found`() {
        // given
        val request =
            CreateMatchRequestApiRequest(
                teamId = 999L,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )

        every { matchingService.createRequest(any()) } throws TeamNotFoundException(999L)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/match-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should get open match requests`() {
        // given
        every { matchingService.getOpenRequests(any<MatchRequestFilterDto>()) } returns listOf(mockMatchRequest)

        // when & then
        mockMvc
            .perform(get("/api/v1/match-requests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].status").value("OPEN"))

        verify(exactly = 1) { matchingService.getOpenRequests(any<MatchRequestFilterDto>()) }
    }

    @Test
    fun `should get empty list when no open requests`() {
        // given
        every { matchingService.getOpenRequests(any<MatchRequestFilterDto>()) } returns emptyList()

        // when & then
        mockMvc
            .perform(get("/api/v1/match-requests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)

        verify(exactly = 1) { matchingService.getOpenRequests(any<MatchRequestFilterDto>()) }
    }

    @Test
    fun `should get match request detail with responses`() {
        // given
        every { matchingService.getRequestById(1L) } returns mockMatchRequest
        every { matchingService.getResponsesByRequest(1L) } returns listOf(mockMatchResponse)

        // when & then
        mockMvc
            .perform(get("/api/v1/match-requests/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.responses").isArray)
            .andExpect(jsonPath("$.data.responses[0].id").value(1))
            .andExpect(jsonPath("$.data.responses[0].respondTeamName").value("응답 팀"))

        verify(exactly = 1) { matchingService.getRequestById(1L) }
        verify(exactly = 1) { matchingService.getResponsesByRequest(1L) }
    }

    @Test
    fun `should fail to get match request detail when not found`() {
        // given
        every { matchingService.getRequestById(999L) } throws MatchRequestNotFoundException(999L)

        // when & then
        mockMvc
            .perform(get("/api/v1/match-requests/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should respond to match request successfully`() {
        // given
        val request =
            RespondToMatchRequestApiRequest(
                respondTeamId = 2L,
                message = "함께 경기하고 싶습니다",
            )

        every { matchingService.respondToRequest(any()) } returns mockMatchResponse

        // when & then
        mockMvc
            .perform(
                post("/api/v1/match-requests/1/respond")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.respondTeamId").value(2))
            .andExpect(jsonPath("$.data.status").value("PENDING"))

        verify(exactly = 1) { matchingService.respondToRequest(any()) }
    }

    @Test
    fun `should fail to respond with invalid data`() {
        // given - send invalid JSON that cannot be deserialized
        val invalidJson = """{"respondTeamId": "not-a-number"}"""

        // when & then
        mockMvc
            .perform(
                post("/api/v1/match-requests/1/respond")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson),
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))

        verify(exactly = 0) { matchingService.respondToRequest(any()) }
    }

    @Test
    fun `should accept match response successfully`() {
        // given
        every { matchingService.acceptResponse(1L, 1L) } returns mockMatchRequest

        // when & then
        mockMvc
            .perform(put("/api/v1/match-requests/1/accept/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))

        verify(exactly = 1) { matchingService.acceptResponse(1L, 1L) }
    }

    @Test
    fun `should cancel match request successfully`() {
        // given
        every { mockMatchRequest.status } returns MatchRequestStatus.CANCELLED
        every { matchingService.cancelRequest(1L) } returns mockMatchRequest

        // when & then
        mockMvc
            .perform(delete("/api/v1/match-requests/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.status").value("CANCELLED"))

        verify(exactly = 1) { matchingService.cancelRequest(1L) }
    }

    @Test
    fun `should fail to cancel when request not found`() {
        // given
        every { matchingService.cancelRequest(999L) } throws MatchRequestNotFoundException(999L)

        // when & then
        mockMvc
            .perform(delete("/api/v1/match-requests/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should handle all skill levels`() {
        // given
        val skillLevels = listOf(SkillLevel.BEGINNER, SkillLevel.INTERMEDIATE, SkillLevel.ADVANCED, SkillLevel.ANY)

        skillLevels.forEach { skillLevel ->
            val request =
                CreateMatchRequestApiRequest(
                    teamId = 1L,
                    preferredDate = LocalDate.now().plusDays(7),
                    preferredTime = null,
                    preferredLocation = null,
                    message = null,
                    skillLevel = skillLevel,
                )

            val mockRequestWithSkill = mockk<MatchRequest>(relaxed = true)
            every { mockRequestWithSkill.id } returns 1L
            every { mockRequestWithSkill.team } returns mockTeam
            every { mockRequestWithSkill.preferredDate } returns LocalDate.now().plusDays(7)
            every { mockRequestWithSkill.preferredTime } returns null
            every { mockRequestWithSkill.preferredLocation } returns null
            every { mockRequestWithSkill.message } returns null
            every { mockRequestWithSkill.skillLevel } returns skillLevel
            every { mockRequestWithSkill.status } returns MatchRequestStatus.OPEN
            every { mockRequestWithSkill.createdAt } returns Instant.now()

            every { matchingService.createRequest(any()) } returns mockRequestWithSkill

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/match-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skillLevel").value(skillLevel.name))
        }
    }
}
