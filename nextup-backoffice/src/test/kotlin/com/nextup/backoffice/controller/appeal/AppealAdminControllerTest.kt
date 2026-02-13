package com.nextup.backoffice.controller.appeal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.backoffice.dto.appeal.ApproveAppealAdminRequest
import com.nextup.backoffice.dto.appeal.RejectAppealAdminRequest
import com.nextup.core.domain.appeal.Appeal
import com.nextup.core.domain.appeal.AppealStatus
import com.nextup.core.domain.appeal.AppealType
import com.nextup.core.domain.game.Game
import com.nextup.core.service.appeal.AppealService
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
import java.time.LocalDateTime

@DisplayName("AppealAdminController")
class AppealAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var appealService: AppealService
    private lateinit var objectMapper: ObjectMapper

    private val mockGame = mockk<Game>(relaxed = true)
    private val mockAppeal = mockk<Appeal>(relaxed = true)

    @BeforeEach
    fun setUp() {
        appealService = mockk()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        val controller = AppealAdminController(appealService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .build()

        every { mockGame.id } returns 1L
        every { mockAppeal.id } returns 1L
        every { mockAppeal.game } returns mockGame
        every { mockAppeal.appealerId } returns 100L
        every { mockAppeal.appealerName } returns "홍길동"
        every { mockAppeal.type } returns AppealType.SCORING_ERROR
        every { mockAppeal.title } returns "득점 오류 정정 요청"
        every { mockAppeal.description } returns "3회말 득점이 잘못 기록되었습니다"
        every { mockAppeal.status } returns AppealStatus.PENDING
        every { mockAppeal.reviewerId } returns null
        every { mockAppeal.reviewerComment } returns null
        every { mockAppeal.reviewedAt } returns null
        every { mockAppeal.createdAt } returns Instant.now()
        every { mockAppeal.updatedAt } returns Instant.now()
    }

    @Test
    fun `should get all appeals`() {
        // given
        every { appealService.getAllAppeals() } returns listOf(mockAppeal)

        // when & then
        mockMvc
            .perform(get("/api/backoffice/appeals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"))

        verify(exactly = 1) { appealService.getAllAppeals() }
    }

    @Test
    fun `should get appeals by status`() {
        // given
        every { appealService.getAppealsByStatus(AppealStatus.PENDING) } returns listOf(mockAppeal)

        // when & then
        mockMvc
            .perform(get("/api/backoffice/appeals").param("status", "PENDING"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].status").value("PENDING"))

        verify(exactly = 1) { appealService.getAppealsByStatus(AppealStatus.PENDING) }
        verify(exactly = 0) { appealService.getAllAppeals() }
    }

    @Test
    fun `should get empty list when no appeals found`() {
        // given
        every { appealService.getAllAppeals() } returns emptyList()

        // when & then
        mockMvc
            .perform(get("/api/backoffice/appeals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `should get appeal by id`() {
        // given
        every { appealService.getById(1L) } returns mockAppeal

        // when & then
        mockMvc
            .perform(get("/api/backoffice/appeals/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.appealerId").value(100))
            .andExpect(jsonPath("$.data.status").value("PENDING"))

        verify(exactly = 1) { appealService.getById(1L) }
    }

    @Test
    fun `should approve appeal successfully`() {
        // given
        val request = ApproveAppealAdminRequest(reviewerId = 200L, comment = "검토 완료")

        val approvedAppeal = mockk<Appeal>(relaxed = true)
        every { approvedAppeal.id } returns 1L
        every { approvedAppeal.game } returns mockGame
        every { approvedAppeal.appealerId } returns 100L
        every { approvedAppeal.appealerName } returns "홍길동"
        every { approvedAppeal.type } returns AppealType.SCORING_ERROR
        every { approvedAppeal.title } returns "득점 오류 정정 요청"
        every { approvedAppeal.description } returns "설명"
        every { approvedAppeal.status } returns AppealStatus.APPROVED
        every { approvedAppeal.reviewerId } returns 200L
        every { approvedAppeal.reviewerComment } returns "검토 완료"
        every { approvedAppeal.reviewedAt } returns LocalDateTime.now()
        every { approvedAppeal.createdAt } returns Instant.now()
        every { approvedAppeal.updatedAt } returns Instant.now()

        every {
            appealService.approveAppeal(
                appealId = 1L,
                reviewerId = 200L,
                comment = "검토 완료",
            )
        } returns approvedAppeal

        // when & then
        mockMvc
            .perform(
                put("/api/backoffice/appeals/1/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.status").value("APPROVED"))
            .andExpect(jsonPath("$.data.reviewerId").value(200))
            .andExpect(jsonPath("$.data.reviewerComment").value("검토 완료"))

        verify(exactly = 1) {
            appealService.approveAppeal(
                appealId = 1L,
                reviewerId = 200L,
                comment = "검토 완료",
            )
        }
    }

    @Test
    fun `should approve appeal without comment`() {
        // given
        val request = ApproveAppealAdminRequest(reviewerId = 200L, comment = null)

        val approvedAppeal = mockk<Appeal>(relaxed = true)
        every { approvedAppeal.id } returns 1L
        every { approvedAppeal.game } returns mockGame
        every { approvedAppeal.appealerId } returns 100L
        every { approvedAppeal.appealerName } returns "홍길동"
        every { approvedAppeal.type } returns AppealType.SCORING_ERROR
        every { approvedAppeal.title } returns "제목"
        every { approvedAppeal.description } returns "설명"
        every { approvedAppeal.status } returns AppealStatus.APPROVED
        every { approvedAppeal.reviewerId } returns 200L
        every { approvedAppeal.reviewerComment } returns null
        every { approvedAppeal.reviewedAt } returns LocalDateTime.now()
        every { approvedAppeal.createdAt } returns Instant.now()
        every { approvedAppeal.updatedAt } returns Instant.now()

        every { appealService.approveAppeal(1L, 200L, null) } returns approvedAppeal

        // when & then
        mockMvc
            .perform(
                put("/api/backoffice/appeals/1/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("APPROVED"))
            .andExpect(jsonPath("$.data.reviewerComment").isEmpty)
    }

    @Test
    fun `should reject appeal successfully`() {
        // given
        val request = RejectAppealAdminRequest(reviewerId = 200L, comment = "증거 불충분")

        val rejectedAppeal = mockk<Appeal>(relaxed = true)
        every { rejectedAppeal.id } returns 1L
        every { rejectedAppeal.game } returns mockGame
        every { rejectedAppeal.appealerId } returns 100L
        every { rejectedAppeal.appealerName } returns "홍길동"
        every { rejectedAppeal.type } returns AppealType.SCORING_ERROR
        every { rejectedAppeal.title } returns "제목"
        every { rejectedAppeal.description } returns "설명"
        every { rejectedAppeal.status } returns AppealStatus.REJECTED
        every { rejectedAppeal.reviewerId } returns 200L
        every { rejectedAppeal.reviewerComment } returns "증거 불충분"
        every { rejectedAppeal.reviewedAt } returns LocalDateTime.now()
        every { rejectedAppeal.createdAt } returns Instant.now()
        every { rejectedAppeal.updatedAt } returns Instant.now()

        every { appealService.rejectAppeal(1L, 200L, "증거 불충분") } returns rejectedAppeal

        // when & then
        mockMvc
            .perform(
                put("/api/backoffice/appeals/1/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.reviewerId").value(200))
            .andExpect(jsonPath("$.data.reviewerComment").value("증거 불충분"))

        verify(exactly = 1) { appealService.rejectAppeal(1L, 200L, "증거 불충분") }
    }

    @Test
    fun `should handle different appeal statuses`() {
        // given
        val pendingAppeal = mockk<Appeal>(relaxed = true)
        val approvedAppeal = mockk<Appeal>(relaxed = true)
        val rejectedAppeal = mockk<Appeal>(relaxed = true)

        every { pendingAppeal.status } returns AppealStatus.PENDING
        every { approvedAppeal.status } returns AppealStatus.APPROVED
        every { rejectedAppeal.status } returns AppealStatus.REJECTED

        every { appealService.getAppealsByStatus(AppealStatus.PENDING) } returns listOf(pendingAppeal)
        every { appealService.getAppealsByStatus(AppealStatus.APPROVED) } returns listOf(approvedAppeal)
        every { appealService.getAppealsByStatus(AppealStatus.REJECTED) } returns listOf(rejectedAppeal)

        // when & then
        listOf(AppealStatus.PENDING, AppealStatus.APPROVED, AppealStatus.REJECTED).forEach { status ->
            mockMvc
                .perform(get("/api/backoffice/appeals").param("status", status.name))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)

            verify { appealService.getAppealsByStatus(status) }
        }
    }
}
