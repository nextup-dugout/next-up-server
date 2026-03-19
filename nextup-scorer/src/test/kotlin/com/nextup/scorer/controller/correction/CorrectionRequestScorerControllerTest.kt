package com.nextup.scorer.controller.correction

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.core.domain.game.CorrectionRequest
import com.nextup.core.domain.game.CorrectionRequestStatus
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.service.game.correction.CorrectionRequestService
import com.nextup.scorer.dto.correction.CreateCorrectionRequestDto
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
import java.time.Instant

@DisplayName("CorrectionRequestScorerController 테스트")
class CorrectionRequestScorerControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var correctionRequestService: CorrectionRequestService
    private lateinit var objectMapper: ObjectMapper

    private val scorerUserId = 10L
    private val mockRequest = mockk<CorrectionRequest>(relaxed = true)

    @BeforeEach
    fun setUp() {
        correctionRequestService = mockk()
        objectMapper =
            jacksonObjectMapper().registerModule(JavaTimeModule())

        // @AuthenticationPrincipal userId: Long 을 위한 보안 컨텍스트 설정
        val authentication =
            UsernamePasswordAuthenticationToken(
                scorerUserId,
                null,
                emptyList(),
            )
        SecurityContextHolder.getContext().authentication = authentication

        val controller =
            CorrectionRequestScorerController(correctionRequestService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                    AuthenticationPrincipalArgumentResolver(),
                )
                .build()

        every { mockRequest.id } returns 1L
        every { mockRequest.gameId } returns 100L
        every { mockRequest.requesterUserId } returns scorerUserId
        every { mockRequest.correctionType } returns CorrectionType.BATTING
        every { mockRequest.targetRecordId } returns 50L
        every { mockRequest.fieldName } returns "hits"
        every { mockRequest.newValue } returns "3"
        every { mockRequest.reason } returns "안타 수 오기록"
        every { mockRequest.status } returns CorrectionRequestStatus.PENDING
        every { mockRequest.reviewerUserId } returns null
        every { mockRequest.reviewComment } returns null
        every { mockRequest.reviewedAt } returns null
        every { mockRequest.createdAt } returns Instant.now()
        every { mockRequest.updatedAt } returns Instant.now()
    }

    @Nested
    @DisplayName("POST /api/scorer/corrections/requests")
    inner class CreateCorrectionRequest {
        @Test
        fun `should create correction request successfully`() {
            // given
            val dto =
                CreateCorrectionRequestDto(
                    gameId = 100L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 50L,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "안타 수 오기록",
                )

            every {
                correctionRequestService.createRequest(
                    gameId = 100L,
                    requesterUserId = scorerUserId,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 50L,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "안타 수 오기록",
                )
            } returns mockRequest

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/scorer/corrections/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.gameId").value(100))
                .andExpect(jsonPath("$.data.correctionType").value("BATTING"))
                .andExpect(jsonPath("$.data.fieldName").value("hits"))
                .andExpect(jsonPath("$.data.newValue").value("3"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.statusDisplayName").value("대기"))
        }
    }

    @Nested
    @DisplayName("GET /api/scorer/corrections/requests/games/{gameId}")
    inner class GetCorrectionRequestsByGame {
        @Test
        fun `should return correction requests for game`() {
            // given
            val gameId = 100L
            every {
                correctionRequestService.getByGameId(gameId)
            } returns listOf(mockRequest)

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/scorer/corrections/requests/games/$gameId"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].gameId").value(100))
                .andExpect(jsonPath("$.data[0].reason").value("안타 수 오기록"))

            verify { correctionRequestService.getByGameId(gameId) }
        }

        @Test
        fun `should return empty list when no requests for game`() {
            // given
            val gameId = 999L
            every {
                correctionRequestService.getByGameId(gameId)
            } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/scorer/corrections/requests/games/$gameId"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }
}
