package com.nextup.api.controller.recruitment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.recruitment.ApplyRecruitmentApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.DuplicateApplicationException
import com.nextup.common.exception.RecruitmentApplicationNotFoundException
import com.nextup.common.exception.RecruitmentNotOpenException
import com.nextup.core.domain.recruitment.ApplicationStatus
import com.nextup.core.domain.recruitment.RecruitmentApplication
import com.nextup.core.domain.recruitment.TeamRecruitment
import com.nextup.core.service.recruitment.RecruitmentApplicationService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

@DisplayName("RecruitmentApplicationController")
class RecruitmentApplicationControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var applicationService: RecruitmentApplicationService
    private lateinit var objectMapper: ObjectMapper

    private val mockRecruitment = mockk<TeamRecruitment>(relaxed = true)
    private val mockApplication = mockk<RecruitmentApplication>(relaxed = true)

    @BeforeEach
    fun setUp() {
        applicationService = mockk()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        val controller = RecruitmentApplicationController(applicationService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        every { mockRecruitment.id } returns 1L
        every { mockRecruitment.title } returns "투수 모집"

        every { mockApplication.id } returns 1L
        every { mockApplication.recruitment } returns mockRecruitment
        every { mockApplication.applicantId } returns 10L
        every { mockApplication.message } returns "열심히 하겠습니다"
        every { mockApplication.preferredPositions } returns "투수,포수"
        every { mockApplication.status } returns ApplicationStatus.PENDING
        every { mockApplication.appliedAt } returns Instant.now()
        every { mockApplication.processedAt } returns null
        every { mockApplication.processedBy } returns null
        every { mockApplication.createdAt } returns Instant.now()
        every { mockApplication.updatedAt } returns Instant.now()
    }

    @Test
    fun `should apply to recruitment`() {
        // given
        val request =
            ApplyRecruitmentApiRequest(
                message = "열심히 하겠습니다",
                preferredPositions = "투수,포수",
            )

        every { applicationService.apply(any()) } returns mockApplication

        // when & then
        mockMvc
            .perform(
                post("/api/v1/recruitments/1/apply")
                    .param("applicantId", "10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.applicantId").value(10))
            .andExpect(jsonPath("$.data.message").value("열심히 하겠습니다"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))

        verify(exactly = 1) { applicationService.apply(any()) }
    }

    @Test
    fun `should return 400 when applying with blank message`() {
        // given
        val invalidRequest =
            mapOf(
                "message" to "",
                "preferredPositions" to "투수",
            )

        // when & then
        mockMvc
            .perform(
                post("/api/v1/recruitments/1/apply")
                    .param("applicantId", "10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))

        verify(exactly = 0) { applicationService.apply(any()) }
    }

    @Test
    fun `should return error when recruitment is not open`() {
        // given
        val request =
            ApplyRecruitmentApiRequest(
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { applicationService.apply(any()) } throws RecruitmentNotOpenException(1L)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/recruitments/1/apply")
                    .param("applicantId", "10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should return error when duplicate application`() {
        // given
        val request =
            ApplyRecruitmentApiRequest(
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { applicationService.apply(any()) } throws DuplicateApplicationException(1L, 10L)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/recruitments/1/apply")
                    .param("applicantId", "10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should get my applications`() {
        // given
        every { applicationService.getApplicationsByApplicant(10L) } returns listOf(mockApplication)

        // when & then
        mockMvc
            .perform(
                get("/api/v1/me/applications")
                    .param("applicantId", "10"),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].applicantId").value(10))

        verify(exactly = 1) { applicationService.getApplicationsByApplicant(10L) }
    }

    @Test
    fun `should get empty list when no applications`() {
        // given
        every { applicationService.getApplicationsByApplicant(10L) } returns emptyList()

        // when & then
        mockMvc
            .perform(
                get("/api/v1/me/applications")
                    .param("applicantId", "10"),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `should withdraw application`() {
        // given
        justRun { applicationService.withdrawApplication(1L, 10L) }

        // when & then
        mockMvc
            .perform(
                delete("/api/v1/me/applications/1")
                    .param("applicantId", "10"),
            )
            .andExpect(status().isNoContent)
            .andExpect(jsonPath("$.success").value(true))

        verify(exactly = 1) { applicationService.withdrawApplication(1L, 10L) }
    }

    @Test
    fun `should return 404 when withdrawing non-existent application`() {
        // given
        every {
            applicationService.withdrawApplication(999L, 10L)
        } throws RecruitmentApplicationNotFoundException(999L)

        // when & then
        mockMvc
            .perform(
                delete("/api/v1/me/applications/999")
                    .param("applicantId", "10"),
            )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should get applications for recruitment`() {
        // given
        every { applicationService.getApplicationsByRecruitment(1L) } returns listOf(mockApplication)

        // when & then
        mockMvc
            .perform(get("/api/v1/recruitments/1/applications"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].status").value("PENDING"))

        verify(exactly = 1) { applicationService.getApplicationsByRecruitment(1L) }
    }

    @Test
    fun `should accept application`() {
        // given
        val acceptedApp = mockk<RecruitmentApplication>(relaxed = true)
        every { acceptedApp.id } returns 1L
        every { acceptedApp.recruitment } returns mockRecruitment
        every { acceptedApp.applicantId } returns 10L
        every { acceptedApp.message } returns "열심히 하겠습니다"
        every { acceptedApp.preferredPositions } returns "투수,포수"
        every { acceptedApp.status } returns ApplicationStatus.ACCEPTED
        every { acceptedApp.appliedAt } returns Instant.now()
        every { acceptedApp.processedAt } returns Instant.now()
        every { acceptedApp.processedBy } returns 100L
        every { acceptedApp.createdAt } returns Instant.now()
        every { acceptedApp.updatedAt } returns Instant.now()

        every { applicationService.acceptApplication(1L, 100L) } returns acceptedApp

        // when & then
        mockMvc
            .perform(
                patch("/api/v1/recruitments/1/applications/1/accept")
                    .param("processorId", "100"),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.data.processedBy").value(100))

        verify(exactly = 1) { applicationService.acceptApplication(1L, 100L) }
    }

    @Test
    fun `should reject application`() {
        // given
        val rejectedApp = mockk<RecruitmentApplication>(relaxed = true)
        every { rejectedApp.id } returns 1L
        every { rejectedApp.recruitment } returns mockRecruitment
        every { rejectedApp.applicantId } returns 10L
        every { rejectedApp.message } returns "열심히 하겠습니다"
        every { rejectedApp.preferredPositions } returns "투수,포수"
        every { rejectedApp.status } returns ApplicationStatus.REJECTED
        every { rejectedApp.appliedAt } returns Instant.now()
        every { rejectedApp.processedAt } returns Instant.now()
        every { rejectedApp.processedBy } returns 100L
        every { rejectedApp.createdAt } returns Instant.now()
        every { rejectedApp.updatedAt } returns Instant.now()

        every { applicationService.rejectApplication(1L, 100L) } returns rejectedApp

        // when & then
        mockMvc
            .perform(
                patch("/api/v1/recruitments/1/applications/1/reject")
                    .param("processorId", "100"),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("REJECTED"))

        verify(exactly = 1) { applicationService.rejectApplication(1L, 100L) }
    }

    @Test
    fun `should return 404 when accepting non-existent application`() {
        // given
        every {
            applicationService.acceptApplication(999L, 100L)
        } throws RecruitmentApplicationNotFoundException(999L)

        // when & then
        mockMvc
            .perform(
                patch("/api/v1/recruitments/1/applications/999/accept")
                    .param("processorId", "100"),
            )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }
}
