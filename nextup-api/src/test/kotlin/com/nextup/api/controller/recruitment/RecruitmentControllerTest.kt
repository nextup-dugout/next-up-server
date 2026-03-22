package com.nextup.api.controller.recruitment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.recruitment.CreateRecruitmentApiRequest
import com.nextup.api.dto.recruitment.UpdateRecruitmentApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.RecruitmentNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.recruitment.RecruitmentStatus
import com.nextup.core.domain.recruitment.TeamRecruitment
import com.nextup.core.domain.team.Team
import com.nextup.core.service.recruitment.TeamRecruitmentService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.LocalDate

@DisplayName("RecruitmentController")
class RecruitmentControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var recruitmentService: TeamRecruitmentService
    private lateinit var objectMapper: ObjectMapper

    private val mockTeam = mockk<Team>(relaxed = true)
    private val mockRecruitment = mockk<TeamRecruitment>(relaxed = true)

    @BeforeEach
    fun setUp() {
        recruitmentService = mockk()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(1L, null, emptyList())

        val controller = RecruitmentController(recruitmentService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .build()

        every { mockTeam.id } returns 1L
        every { mockTeam.name } returns "테스트 팀"

        every { mockRecruitment.id } returns 1L
        every { mockRecruitment.team } returns mockTeam
        every { mockRecruitment.title } returns "투수 모집"
        every { mockRecruitment.description } returns "경험 많은 투수를 모집합니다"
        every { mockRecruitment.positionsNeeded } returns "투수,포수"
        every { mockRecruitment.ageRange } returns "20-35"
        every { mockRecruitment.skillLevel } returns "중급"
        every { mockRecruitment.location } returns "서울"
        every { mockRecruitment.deadline } returns LocalDate.now().plusDays(30)
        every { mockRecruitment.status } returns RecruitmentStatus.OPEN
        every { mockRecruitment.createdAt } returns Instant.now()
        every { mockRecruitment.updatedAt } returns Instant.now()
    }

    @Test
    fun `should get all open recruitments`() {
        // given
        every { recruitmentService.getAllOpen() } returns listOf(mockRecruitment)

        // when & then
        mockMvc
            .perform(get("/api/v1/recruitments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].title").value("투수 모집"))
            .andExpect(jsonPath("$.data[0].status").value("OPEN"))

        verify(exactly = 1) { recruitmentService.getAllOpen() }
    }

    @Test
    fun `should get empty list when no open recruitments`() {
        // given
        every { recruitmentService.getAllOpen() } returns emptyList()

        // when & then
        mockMvc
            .perform(get("/api/v1/recruitments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)

        verify(exactly = 1) { recruitmentService.getAllOpen() }
    }

    @Test
    fun `should get recruitment by id`() {
        // given
        every { recruitmentService.getById(1L) } returns mockRecruitment

        // when & then
        mockMvc
            .perform(get("/api/v1/recruitments/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("투수 모집"))
            .andExpect(jsonPath("$.data.teamName").value("테스트 팀"))

        verify(exactly = 1) { recruitmentService.getById(1L) }
    }

    @Test
    fun `should return 404 when recruitment not found`() {
        // given
        every { recruitmentService.getById(999L) } throws RecruitmentNotFoundException(999L)

        // when & then
        mockMvc
            .perform(get("/api/v1/recruitments/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should create recruitment`() {
        // given
        val request =
            CreateRecruitmentApiRequest(
                title = "투수 모집",
                description = "경험 많은 투수를 모집합니다",
                positionsNeeded = "투수,포수",
                ageRange = "20-35",
                skillLevel = "중급",
                location = "서울",
                deadline = LocalDate.now().plusDays(30),
            )

        every { recruitmentService.createRecruitment(any()) } returns mockRecruitment

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/1/recruitments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("투수 모집"))
            .andExpect(jsonPath("$.data.status").value("OPEN"))

        verify(exactly = 1) { recruitmentService.createRecruitment(any()) }
    }

    @Test
    fun `should return 400 when creating recruitment with invalid data`() {
        // given
        val invalidRequest =
            mapOf(
                "title" to "",
                "description" to "설명",
                "positionsNeeded" to "투수",
                "deadline" to LocalDate.now().plusDays(30).toString(),
            )

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/1/recruitments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))

        verify(exactly = 0) { recruitmentService.createRecruitment(any()) }
    }

    @Test
    fun `should return 404 when creating recruitment with non-existent team`() {
        // given
        val request =
            CreateRecruitmentApiRequest(
                title = "투수 모집",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        every { recruitmentService.createRecruitment(any()) } throws TeamNotFoundException(999L)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/999/recruitments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should update recruitment`() {
        // given
        val request =
            UpdateRecruitmentApiRequest(
                title = "새 제목",
                description = "새 설명",
                positionsNeeded = "투수,포수,내야수",
                deadline = LocalDate.now().plusDays(60),
            )

        val updatedRecruitment = mockk<TeamRecruitment>(relaxed = true)
        every { updatedRecruitment.id } returns 1L
        every { updatedRecruitment.team } returns mockTeam
        every { updatedRecruitment.title } returns "새 제목"
        every { updatedRecruitment.description } returns "새 설명"
        every { updatedRecruitment.positionsNeeded } returns "투수,포수,내야수"
        every { updatedRecruitment.ageRange } returns null
        every { updatedRecruitment.skillLevel } returns null
        every { updatedRecruitment.location } returns null
        every { updatedRecruitment.deadline } returns LocalDate.now().plusDays(60)
        every { updatedRecruitment.status } returns RecruitmentStatus.OPEN
        every { updatedRecruitment.createdAt } returns Instant.now()
        every { updatedRecruitment.updatedAt } returns Instant.now()

        every { recruitmentService.updateRecruitment(any(), any(), any()) } returns updatedRecruitment

        // when & then
        mockMvc
            .perform(
                put("/api/v1/teams/1/recruitments/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("새 제목"))

        verify(exactly = 1) { recruitmentService.updateRecruitment(any(), any(), any()) }
    }

    @Test
    fun `should delete recruitment`() {
        // given
        justRun { recruitmentService.deleteRecruitment(1L, 1L) }

        // when & then
        mockMvc
            .perform(delete("/api/v1/teams/1/recruitments/1"))
            .andExpect(status().isNoContent)
            .andExpect(jsonPath("$.success").value(true))

        verify(exactly = 1) { recruitmentService.deleteRecruitment(1L, 1L) }
    }

    @Test
    fun `should return 404 when deleting non-existent recruitment`() {
        // given
        every { recruitmentService.deleteRecruitment(999L, 1L) } throws RecruitmentNotFoundException(999L)

        // when & then
        mockMvc
            .perform(delete("/api/v1/teams/1/recruitments/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }
}
