package com.nextup.api.controller.team

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.team.CreateTeamScheduleRequest
import com.nextup.api.dto.team.UpdateTeamScheduleRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamSchedule
import com.nextup.core.domain.team.TeamScheduleType
import com.nextup.core.service.team.TeamScheduleService
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
import java.time.Instant
import java.time.LocalDateTime

@DisplayName("TeamScheduleController 테스트")
class TeamScheduleControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var teamScheduleService: TeamScheduleService
    private lateinit var objectMapper: ObjectMapper

    private val teamId = 1L
    private val scheduleId = 10L

    private val mockTeam = mockk<Team>(relaxed = true)
    private val mockSchedule = mockk<TeamSchedule>(relaxed = true)

    @BeforeEach
    fun setUp() {
        teamScheduleService = mockk()
        objectMapper =
            jacksonObjectMapper().registerModule(JavaTimeModule())

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(1L, null, emptyList())

        val controller = TeamScheduleController(teamScheduleService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .build()

        every { mockTeam.id } returns teamId
        every { mockTeam.name } returns "테스트팀"
        every { mockSchedule.id } returns scheduleId
        every { mockSchedule.team } returns mockTeam
        every { mockSchedule.title } returns "연습 일정"
        every { mockSchedule.description } returns "정기 연습"
        every { mockSchedule.scheduleType } returns TeamScheduleType.PRACTICE
        every { mockSchedule.startAt } returns LocalDateTime.of(2024, 6, 1, 14, 0)
        every { mockSchedule.endAt } returns LocalDateTime.of(2024, 6, 1, 17, 0)
        every { mockSchedule.location } returns "서울 야구장"
        every { mockSchedule.createdAt } returns Instant.now()
        every { mockSchedule.updatedAt } returns Instant.now()
    }

    @Nested
    @DisplayName("POST /api/v1/teams/{teamId}/schedules")
    inner class CreateSchedule {
        @Test
        fun `should create team schedule successfully`() {
            // given
            val request =
                CreateTeamScheduleRequest(
                    title = "연습 일정",
                    description = "정기 연습",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = LocalDateTime.of(2024, 6, 1, 14, 0),
                    endAt = LocalDateTime.of(2024, 6, 1, 17, 0),
                    location = "서울 야구장",
                )

            every {
                teamScheduleService.create(
                    teamId = teamId,
                    title = request.title,
                    description = request.description,
                    scheduleType = request.scheduleType,
                    startAt = request.startAt,
                    endAt = request.endAt,
                    location = request.location,
                )
            } returns mockSchedule

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/teams/$teamId/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(scheduleId))
                .andExpect(jsonPath("$.data.teamId").value(teamId))
                .andExpect(jsonPath("$.data.title").value("연습 일정"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/schedules")
    inner class GetSchedules {
        @Test
        fun `should return all schedules for team`() {
            // given
            every { teamScheduleService.getByTeamId(teamId) } returns listOf(mockSchedule)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/$teamId/schedules"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(scheduleId))
                .andExpect(jsonPath("$.data[0].title").value("연습 일정"))

            verify { teamScheduleService.getByTeamId(teamId) }
        }

        @Test
        fun `should return filtered schedules when date range provided`() {
            // given
            val from = LocalDateTime.of(2024, 6, 1, 0, 0)
            val to = LocalDateTime.of(2024, 6, 30, 23, 59)

            every {
                teamScheduleService.getByTeamIdAndDateRange(teamId, from, to)
            } returns listOf(mockSchedule)

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/teams/$teamId/schedules")
                        .param("from", "2024-06-01T00:00:00")
                        .param("to", "2024-06-30T23:59:00"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(scheduleId))

            verify {
                teamScheduleService.getByTeamIdAndDateRange(teamId, from, to)
            }
        }

        @Test
        fun `should return empty list when no schedules exist`() {
            // given
            every { teamScheduleService.getByTeamId(teamId) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/$teamId/schedules"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/schedules/{scheduleId}")
    inner class GetSchedule {
        @Test
        fun `should return schedule detail`() {
            // given
            every { teamScheduleService.getById(scheduleId) } returns mockSchedule

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/$teamId/schedules/$scheduleId"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(scheduleId))
                .andExpect(jsonPath("$.data.title").value("연습 일정"))
                .andExpect(jsonPath("$.data.location").value("서울 야구장"))

            verify { teamScheduleService.getById(scheduleId) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/teams/{teamId}/schedules/{scheduleId}")
    inner class UpdateSchedule {
        @Test
        fun `should update schedule successfully`() {
            // given
            val request =
                UpdateTeamScheduleRequest(
                    title = "수정된 연습 일정",
                    location = "부산 야구장",
                )

            val updatedSchedule = mockk<TeamSchedule>(relaxed = true)
            every { updatedSchedule.id } returns scheduleId
            every { updatedSchedule.team } returns mockTeam
            every { updatedSchedule.title } returns "수정된 연습 일정"
            every { updatedSchedule.description } returns "정기 연습"
            every { updatedSchedule.scheduleType } returns TeamScheduleType.PRACTICE
            every { updatedSchedule.startAt } returns LocalDateTime.of(2024, 6, 1, 14, 0)
            every { updatedSchedule.endAt } returns LocalDateTime.of(2024, 6, 1, 17, 0)
            every { updatedSchedule.location } returns "부산 야구장"
            every { updatedSchedule.createdAt } returns Instant.now()
            every { updatedSchedule.updatedAt } returns Instant.now()

            every {
                teamScheduleService.update(
                    id = scheduleId,
                    title = request.title,
                    description = request.description,
                    scheduleType = request.scheduleType,
                    startAt = request.startAt,
                    endAt = request.endAt,
                    location = request.location,
                )
            } returns updatedSchedule

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/teams/$teamId/schedules/$scheduleId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 연습 일정"))
                .andExpect(jsonPath("$.data.location").value("부산 야구장"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/teams/{teamId}/schedules/{scheduleId}")
    inner class DeleteSchedule {
        @Test
        fun `should delete schedule successfully`() {
            // given
            every { teamScheduleService.delete(scheduleId) } returns Unit

            // when & then
            mockMvc
                .perform(delete("/api/v1/teams/$teamId/schedules/$scheduleId"))
                .andExpect(status().isNoContent)
                .andExpect(jsonPath("$.success").value(true))

            verify { teamScheduleService.delete(scheduleId) }
        }
    }
}
