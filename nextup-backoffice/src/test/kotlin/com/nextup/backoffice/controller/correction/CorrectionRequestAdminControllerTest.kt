package com.nextup.backoffice.controller.correction

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.backoffice.dto.correction.ApproveCorrectionRequestDto
import com.nextup.backoffice.dto.correction.RejectCorrectionRequestDto
import com.nextup.core.domain.game.CorrectionRequest
import com.nextup.core.domain.game.CorrectionRequestStatus
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.service.game.correction.CorrectionRequestService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

@DisplayName("CorrectionRequestAdminController 테스트")
class CorrectionRequestAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var correctionRequestService: CorrectionRequestService
    private lateinit var objectMapper: ObjectMapper

    private val adminUserId = 99L
    private val mockRequest = mockk<CorrectionRequest>(relaxed = true)

    @BeforeEach
    fun setUp() {
        correctionRequestService = mockk()
        objectMapper =
            jacksonObjectMapper().registerModule(JavaTimeModule())

        // @AuthenticationPrincipal adminUserId: Long 을 위한 보안 컨텍스트 설정
        val authentication =
            UsernamePasswordAuthenticationToken(
                adminUserId,
                null,
                emptyList(),
            )
        SecurityContextHolder.getContext().authentication = authentication

        val controller =
            CorrectionRequestAdminController(correctionRequestService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                    AuthenticationPrincipalArgumentResolver(),
                )
                .build()

        every { mockRequest.id } returns 1L
        every { mockRequest.gameId } returns 100L
        every { mockRequest.requesterUserId } returns 10L
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
    @DisplayName("GET /api/backoffice/corrections/pending")
    inner class GetPendingRequests {
        @Test
        fun `should return pending correction requests`() {
            // given
            every {
                correctionRequestService.getPendingRequests()
            } returns listOf(mockRequest)

            // when & then
            mockMvc
                .perform(get("/api/backoffice/corrections/pending"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].gameId").value(100))
                .andExpect(jsonPath("$.data[0].correctionType").value("BATTING"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].statusDisplayName").value("대기"))

            verify { correctionRequestService.getPendingRequests() }
        }

        @Test
        fun `should return empty list when no pending requests`() {
            // given
            every {
                correctionRequestService.getPendingRequests()
            } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/backoffice/corrections/pending"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/corrections/{id}")
    inner class GetCorrectionRequest {
        @Test
        fun `should return correction request detail`() {
            // given
            every { correctionRequestService.getById(1L) } returns mockRequest

            // when & then
            mockMvc
                .perform(get("/api/backoffice/corrections/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fieldName").value("hits"))
                .andExpect(jsonPath("$.data.newValue").value("3"))
                .andExpect(jsonPath("$.data.reason").value("안타 수 오기록"))

            verify { correctionRequestService.getById(1L) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/backoffice/corrections/{id}/approve")
    inner class ApproveRequest {
        @Test
        fun `should approve correction request successfully`() {
            // given
            val approvedRequest = mockk<CorrectionRequest>(relaxed = true)
            every { approvedRequest.id } returns 1L
            every { approvedRequest.gameId } returns 100L
            every { approvedRequest.requesterUserId } returns 10L
            every { approvedRequest.correctionType } returns CorrectionType.BATTING
            every { approvedRequest.targetRecordId } returns 50L
            every { approvedRequest.fieldName } returns "hits"
            every { approvedRequest.newValue } returns "3"
            every { approvedRequest.reason } returns "안타 수 오기록"
            every { approvedRequest.status } returns CorrectionRequestStatus.APPROVED
            every { approvedRequest.reviewerUserId } returns adminUserId
            every { approvedRequest.reviewComment } returns "확인 완료"
            every { approvedRequest.reviewedAt } returns Instant.now()
            every { approvedRequest.createdAt } returns Instant.now()
            every { approvedRequest.updatedAt } returns Instant.now()

            val dto = ApproveCorrectionRequestDto(comment = "확인 완료")

            every {
                correctionRequestService.approve(
                    requestId = 1L,
                    reviewerUserId = adminUserId,
                    comment = "확인 완료",
                )
            } returns approvedRequest

            // when & then
            mockMvc
                .perform(
                    patch("/api/backoffice/corrections/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.statusDisplayName").value("승인"))
                .andExpect(jsonPath("$.data.reviewComment").value("확인 완료"))
        }
    }

    @Nested
    @DisplayName("PATCH /api/backoffice/corrections/{id}/reject")
    inner class RejectRequest {
        @Test
        fun `should reject correction request successfully`() {
            // given
            val rejectedRequest = mockk<CorrectionRequest>(relaxed = true)
            every { rejectedRequest.id } returns 1L
            every { rejectedRequest.gameId } returns 100L
            every { rejectedRequest.requesterUserId } returns 10L
            every { rejectedRequest.correctionType } returns CorrectionType.BATTING
            every { rejectedRequest.targetRecordId } returns 50L
            every { rejectedRequest.fieldName } returns "hits"
            every { rejectedRequest.newValue } returns "3"
            every { rejectedRequest.reason } returns "안타 수 오기록"
            every { rejectedRequest.status } returns CorrectionRequestStatus.REJECTED
            every { rejectedRequest.reviewerUserId } returns adminUserId
            every { rejectedRequest.reviewComment } returns "영상 확인 결과 정확합니다"
            every { rejectedRequest.reviewedAt } returns Instant.now()
            every { rejectedRequest.createdAt } returns Instant.now()
            every { rejectedRequest.updatedAt } returns Instant.now()

            val dto =
                RejectCorrectionRequestDto(
                    comment = "영상 확인 결과 정확합니다",
                )

            every {
                correctionRequestService.reject(
                    requestId = 1L,
                    reviewerUserId = adminUserId,
                    comment = "영상 확인 결과 정확합니다",
                )
            } returns rejectedRequest

            // when & then
            mockMvc
                .perform(
                    patch("/api/backoffice/corrections/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.statusDisplayName").value("반려"))
                .andExpect(
                    jsonPath("$.data.reviewComment")
                        .value("영상 확인 결과 정확합니다"),
                )
        }
    }
}
