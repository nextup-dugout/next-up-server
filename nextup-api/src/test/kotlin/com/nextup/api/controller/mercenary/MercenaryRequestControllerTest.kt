package com.nextup.api.controller.mercenary

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.mercenary.ApplyMercenaryApiRequest
import com.nextup.api.dto.mercenary.CreateMercenaryRequestApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.MercenaryApplicationNotFoundException
import com.nextup.common.exception.MercenaryRequestNotFoundException
import com.nextup.core.domain.mercenary.MercenaryApplication
import com.nextup.core.domain.mercenary.MercenaryApplicationStatus
import com.nextup.core.domain.mercenary.MercenaryParticipation
import com.nextup.core.domain.mercenary.MercenaryRequest
import com.nextup.core.domain.mercenary.MercenaryRequestStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.service.mercenary.MercenaryService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

@DisplayName("MercenaryRequestController 테스트")
class MercenaryRequestControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var mercenaryService: MercenaryService
    private lateinit var objectMapper: ObjectMapper

    private val mockRequest = mockk<MercenaryRequest>(relaxed = true)
    private val mockApplication = mockk<MercenaryApplication>(relaxed = true)
    private val mockParticipation = mockk<MercenaryParticipation>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mercenaryService = mockk()
        objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(1L, null, emptyList())

        val controller = MercenaryRequestController(mercenaryService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .build()

        every { mockRequest.id } returns 1L
        every { mockRequest.requestingTeamId } returns 10L
        every { mockRequest.gameId } returns 100L
        every { mockRequest.positions } returns mutableSetOf(Position.STARTING_PITCHER, Position.CATCHER)
        every { mockRequest.maxCount } returns 2
        every { mockRequest.status } returns MercenaryRequestStatus.OPEN
        every { mockRequest.deadline } returns Instant.now().plusSeconds(86400)
        every { mockRequest.description } returns "투수 1명, 포수 1명 구합니다"
        every { mockRequest.createdAt } returns Instant.now()

        every { mockApplication.id } returns 1L
        every { mockApplication.requestId } returns 1L
        every { mockApplication.playerId } returns 50L
        every { mockApplication.preferredPositions } returns mutableSetOf(Position.STARTING_PITCHER)
        every { mockApplication.status } returns MercenaryApplicationStatus.PENDING
        every { mockApplication.message } returns "열심히 하겠습니다"
        every { mockApplication.createdAt } returns Instant.now()

        every { mockParticipation.id } returns 1L
        every { mockParticipation.gameId } returns 100L
        every { mockParticipation.playerId } returns 50L
        every { mockParticipation.teamId } returns 10L
        every { mockParticipation.createdAt } returns Instant.now()
    }

    @Nested
    @DisplayName("POST /api/v1/mercenary-requests - 용병 요청 생성")
    inner class CreateRequest {
        @Test
        fun `용병 요청 생성에 성공한다`() {
            // given
            val request =
                CreateMercenaryRequestApiRequest(
                    teamId = 10L,
                    gameId = 100L,
                    positions = setOf(Position.STARTING_PITCHER, Position.CATCHER),
                    maxCount = 2,
                    deadline = Instant.now().plusSeconds(86400),
                    description = "투수 1명, 포수 1명 구합니다",
                )

            every { mercenaryService.createRequest(any()) } returns mockRequest

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/mercenary-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.requestingTeamId").value(10))
                .andExpect(jsonPath("$.data.gameId").value(100))
                .andExpect(jsonPath("$.data.maxCount").value(2))
                .andExpect(jsonPath("$.data.status").value("OPEN"))

            verify(exactly = 1) { mercenaryService.createRequest(any()) }
        }

        @Test
        fun `필수 필드가 없으면 400을 반환한다`() {
            // given - teamId, gameId, positions, deadline 모두 null
            val invalidRequest =
                mapOf(
                    "teamId" to null,
                    "gameId" to null,
                    "positions" to emptyList<String>(),
                    "maxCount" to 0,
                    "deadline" to null,
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/mercenary-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)),
                )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))

            verify(exactly = 0) { mercenaryService.createRequest(any()) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mercenary-requests - OPEN 용병 요청 목록 조회")
    inner class GetOpenRequests {
        @Test
        fun `OPEN 상태 용병 요청 목록을 반환한다`() {
            // given
            every { mercenaryService.getOpenRequests() } returns listOf(mockRequest)

            // when & then
            mockMvc
                .perform(get("/api/v1/mercenary-requests"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[0].requestingTeamId").value(10))

            verify(exactly = 1) { mercenaryService.getOpenRequests() }
        }

        @Test
        fun `용병 요청이 없으면 빈 목록을 반환한다`() {
            // given
            every { mercenaryService.getOpenRequests() } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/mercenary-requests"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)

            verify(exactly = 1) { mercenaryService.getOpenRequests() }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/mercenary-requests/{id}/apply - 용병 지원")
    inner class Apply {
        @Test
        fun `용병 지원에 성공한다`() {
            // given
            val request =
                ApplyMercenaryApiRequest(
                    playerId = 50L,
                    preferredPositions = setOf(Position.STARTING_PITCHER),
                    message = "열심히 하겠습니다",
                )

            every { mercenaryService.apply(any()) } returns mockApplication

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/mercenary-requests/1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.requestId").value(1))
                .andExpect(jsonPath("$.data.playerId").value(50))
                .andExpect(jsonPath("$.data.status").value("PENDING"))

            verify(exactly = 1) { mercenaryService.apply(any()) }
        }

        @Test
        fun `존재하지 않는 용병 요청에 지원하면 404를 반환한다`() {
            // given
            val request =
                ApplyMercenaryApiRequest(
                    playerId = 50L,
                    preferredPositions = setOf(Position.STARTING_PITCHER),
                    message = null,
                )

            every { mercenaryService.apply(any()) } throws MercenaryRequestNotFoundException(999L)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/mercenary-requests/999/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))

            verify(exactly = 1) { mercenaryService.apply(any()) }
        }

        @Test
        fun `필수 필드가 없으면 400을 반환한다`() {
            // given - playerId null, preferredPositions empty
            val invalidRequest =
                mapOf(
                    "playerId" to null,
                    "preferredPositions" to emptyList<String>(),
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/mercenary-requests/1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)),
                )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))

            verify(exactly = 0) { mercenaryService.apply(any()) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/mercenary-requests/{id}/applications/{appId}/accept - 지원 수락")
    inner class AcceptApplication {
        @Test
        fun `용병 지원 수락에 성공한다`() {
            // given
            val acceptedApplication = mockk<MercenaryApplication>(relaxed = true)
            every { acceptedApplication.id } returns 1L
            every { acceptedApplication.requestId } returns 1L
            every { acceptedApplication.playerId } returns 50L
            every { acceptedApplication.preferredPositions } returns mutableSetOf(Position.STARTING_PITCHER)
            every { acceptedApplication.status } returns MercenaryApplicationStatus.ACCEPTED
            every { acceptedApplication.message } returns null
            every { acceptedApplication.createdAt } returns Instant.now()

            every { mercenaryService.acceptApplication(1L, 1L) } returns acceptedApplication

            // when & then
            mockMvc
                .perform(patch("/api/v1/mercenary-requests/1/applications/1/accept"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))

            verify(exactly = 1) { mercenaryService.acceptApplication(1L, 1L) }
        }

        @Test
        fun `존재하지 않는 지원을 수락하면 404를 반환한다`() {
            // given
            every { mercenaryService.acceptApplication(1L, 999L) } throws MercenaryApplicationNotFoundException(999L)

            // when & then
            mockMvc
                .perform(patch("/api/v1/mercenary-requests/1/applications/999/accept"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))

            verify(exactly = 1) { mercenaryService.acceptApplication(1L, 999L) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/mercenary-requests/{id}/applications/{appId}/reject - 지원 거절")
    inner class RejectApplication {
        @Test
        fun `용병 지원 거절에 성공한다`() {
            // given
            val rejectedApplication = mockk<MercenaryApplication>(relaxed = true)
            every { rejectedApplication.id } returns 1L
            every { rejectedApplication.requestId } returns 1L
            every { rejectedApplication.playerId } returns 50L
            every { rejectedApplication.preferredPositions } returns mutableSetOf(Position.STARTING_PITCHER)
            every { rejectedApplication.status } returns MercenaryApplicationStatus.REJECTED
            every { rejectedApplication.message } returns null
            every { rejectedApplication.createdAt } returns Instant.now()

            every { mercenaryService.rejectApplication(1L, 1L) } returns rejectedApplication

            // when & then
            mockMvc
                .perform(patch("/api/v1/mercenary-requests/1/applications/1/reject"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))

            verify(exactly = 1) { mercenaryService.rejectApplication(1L, 1L) }
        }

        @Test
        fun `존재하지 않는 지원을 거절하면 404를 반환한다`() {
            // given
            every { mercenaryService.rejectApplication(1L, 999L) } throws MercenaryApplicationNotFoundException(999L)

            // when & then
            mockMvc
                .perform(patch("/api/v1/mercenary-requests/1/applications/999/reject"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))

            verify(exactly = 1) { mercenaryService.rejectApplication(1L, 999L) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mercenary-requests/{id}/applications - 지원 목록 조회")
    inner class GetApplications {
        @Test
        fun `용병 요청에 대한 지원 목록을 반환한다`() {
            // given
            every { mercenaryService.getApplicationsByRequest(1L) } returns listOf(mockApplication)

            // when & then
            mockMvc
                .perform(get("/api/v1/mercenary-requests/1/applications"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].playerId").value(50))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))

            verify(exactly = 1) { mercenaryService.getApplicationsByRequest(1L) }
        }

        @Test
        fun `지원이 없으면 빈 목록을 반환한다`() {
            // given
            every { mercenaryService.getApplicationsByRequest(1L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/mercenary-requests/1/applications"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)

            verify(exactly = 1) { mercenaryService.getApplicationsByRequest(1L) }
        }

        @Test
        fun `존재하지 않는 요청의 지원 목록 조회 시 404를 반환한다`() {
            // given
            every { mercenaryService.getApplicationsByRequest(999L) } throws MercenaryRequestNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/v1/mercenary-requests/999/applications"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))

            verify(exactly = 1) { mercenaryService.getApplicationsByRequest(999L) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mercenary-requests/me/history - 내 참가 이력 조회")
    inner class GetMyHistory {
        @Test
        fun `선수의 용병 참가 이력을 반환한다`() {
            // given
            every { mercenaryService.getParticipationsByPlayer(50L) } returns listOf(mockParticipation)

            // when & then
            mockMvc
                .perform(get("/api/v1/mercenary-requests/me/history").param("playerId", "50"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].gameId").value(100))
                .andExpect(jsonPath("$.data[0].playerId").value(50))
                .andExpect(jsonPath("$.data[0].teamId").value(10))

            verify(exactly = 1) { mercenaryService.getParticipationsByPlayer(50L) }
        }

        @Test
        fun `참가 이력이 없으면 빈 목록을 반환한다`() {
            // given
            every { mercenaryService.getParticipationsByPlayer(50L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/mercenary-requests/me/history").param("playerId", "50"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)

            verify(exactly = 1) { mercenaryService.getParticipationsByPlayer(50L) }
        }

        @Test
        fun `playerId 파라미터가 없으면 400을 반환한다`() {
            // when & then
            mockMvc
                .perform(get("/api/v1/mercenary-requests/me/history"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))

            verify(exactly = 0) { mercenaryService.getParticipationsByPlayer(any()) }
        }
    }
}
