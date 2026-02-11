package com.nextup.backoffice.controller.schedule

import com.nextup.core.domain.schedule.ConflictType
import com.nextup.core.domain.schedule.ScheduleConflict
import com.nextup.core.service.schedule.LeagueScheduleService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalTime

class ScheduleAdminControllerValidationTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var scheduleService: LeagueScheduleService

    @BeforeEach
    fun setUp() {
        scheduleService = mockk(relaxed = true)
        val controller = ScheduleAdminController(scheduleService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `should validate schedule and return no conflicts`() {
        // given
        every {
            scheduleService.validateSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 1,
                homeTeamId = 1L,
                awayTeamId = 2L,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )
        } returns emptyList()

        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/validate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "round": 1,
                        "matchNumber": 1,
                        "homeTeamId": 1,
                        "awayTeamId": 2,
                        "scheduledDate": "2024-05-01",
                        "scheduledTime": "14:00:00",
                        "venue": "구장A"
                    }
                    """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
                jsonPath("$.data.length()") { value(0) }
            }
    }

    @Test
    fun `should validate schedule and return team conflicts`() {
        // given
        val conflicts =
            listOf(
                ScheduleConflict(
                    type = ConflictType.TEAM_TIME_CONFLICT,
                    conflictingScheduleId = 100L,
                    description = "홈팀 '팀A'이(가) 2024-05-01 14:00에 다른 경기(팀A vs 팀B)에 배정되어 있습니다.",
                ),
            )

        every {
            scheduleService.validateSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 2,
                homeTeamId = 1L,
                awayTeamId = 3L,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장B",
            )
        } returns conflicts

        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/validate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "round": 1,
                        "matchNumber": 2,
                        "homeTeamId": 1,
                        "awayTeamId": 3,
                        "scheduledDate": "2024-05-01",
                        "scheduledTime": "14:00:00",
                        "venue": "구장B"
                    }
                    """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].type") { value("TEAM_TIME_CONFLICT") }
                jsonPath("$.data[0].conflictingScheduleId") { value(100) }
                jsonPath("$.data[0].description") { value(conflicts[0].description) }
            }
    }

    @Test
    fun `should validate schedule and return venue conflicts`() {
        // given
        val conflicts =
            listOf(
                ScheduleConflict(
                    type = ConflictType.VENUE_TIME_CONFLICT,
                    conflictingScheduleId = 100L,
                    description = "경기장 '구장A'에 2024-05-01 14:00에 다른 경기(팀A vs 팀B)가 배정되어 있습니다.",
                ),
            )

        every {
            scheduleService.validateSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 2,
                homeTeamId = 3L,
                awayTeamId = 4L,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )
        } returns conflicts

        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/validate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "round": 1,
                        "matchNumber": 2,
                        "homeTeamId": 3,
                        "awayTeamId": 4,
                        "scheduledDate": "2024-05-01",
                        "scheduledTime": "14:00:00",
                        "venue": "구장A"
                    }
                    """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].type") { value("VENUE_TIME_CONFLICT") }
            }
    }

    @Test
    fun `should validate schedule and return multiple conflicts`() {
        // given
        val conflicts =
            listOf(
                ScheduleConflict(
                    type = ConflictType.TEAM_TIME_CONFLICT,
                    conflictingScheduleId = 100L,
                    description = "홈팀 '팀A'이(가) 2024-05-01 14:00에 다른 경기(팀A vs 팀B)에 배정되어 있습니다.",
                ),
                ScheduleConflict(
                    type = ConflictType.VENUE_TIME_CONFLICT,
                    conflictingScheduleId = 100L,
                    description = "경기장 '구장A'에 2024-05-01 14:00에 다른 경기(팀A vs 팀B)가 배정되어 있습니다.",
                ),
            )

        every {
            scheduleService.validateSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 2,
                homeTeamId = 1L,
                awayTeamId = 3L,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )
        } returns conflicts

        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/validate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "round": 1,
                        "matchNumber": 2,
                        "homeTeamId": 1,
                        "awayTeamId": 3,
                        "scheduledDate": "2024-05-01",
                        "scheduledTime": "14:00:00",
                        "venue": "구장A"
                    }
                    """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
                jsonPath("$.data.length()") { value(2) }
            }
    }
}
