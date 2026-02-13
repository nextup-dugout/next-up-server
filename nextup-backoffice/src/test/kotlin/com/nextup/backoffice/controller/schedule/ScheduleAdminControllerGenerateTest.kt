package com.nextup.backoffice.controller.schedule

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.league.League
import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.team.Team
import com.nextup.core.service.schedule.LeagueScheduleService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

class ScheduleAdminControllerGenerateTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var scheduleService: LeagueScheduleService

    @BeforeEach
    fun setUp() {
        scheduleService = mockk(relaxed = true)
        val controller = ScheduleAdminController(scheduleService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `should generate round robin schedule for 4 teams`() {
        // given
        val schedules = createSchedules(teamCount = 4, matchCount = 6)

        every {
            scheduleService.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 2L, 3L, 4L),
                doubleRoundRobin = false,
            )
        } returns schedules

        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/generate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "teamIds": [1, 2, 3, 4],
                        "doubleRoundRobin": false
                    }
                    """.trimIndent()
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
                jsonPath("$.data.length()") { value(6) }
            }

        verify {
            scheduleService.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 2L, 3L, 4L),
                doubleRoundRobin = false,
            )
        }
    }

    @Test
    fun `should generate double round robin schedule`() {
        // given
        val schedules = createSchedules(teamCount = 3, matchCount = 6)

        every {
            scheduleService.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 2L, 3L),
                doubleRoundRobin = true,
            )
        } returns schedules

        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/generate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "teamIds": [1, 2, 3],
                        "doubleRoundRobin": true
                    }
                    """.trimIndent()
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
                jsonPath("$.data.length()") { value(6) }
            }

        verify {
            scheduleService.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 2L, 3L),
                doubleRoundRobin = true,
            )
        }
    }

    @Test
    fun `should generate schedule with default doubleRoundRobin false`() {
        // given
        val schedules = createSchedules(teamCount = 3, matchCount = 3)

        every {
            scheduleService.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 2L, 3L),
                doubleRoundRobin = false,
            )
        } returns schedules

        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/generate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "teamIds": [1, 2, 3]
                    }
                    """.trimIndent()
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
                jsonPath("$.data.length()") { value(3) }
            }
    }

    @Test
    fun `should return 400 when teamIds is empty`() {
        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/generate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "teamIds": [],
                        "doubleRoundRobin": false
                    }
                    """.trimIndent()
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `should return 400 when teamIds is missing`() {
        // when & then
        mockMvc
            .post("/api/backoffice/competitions/1/schedule/generate") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "doubleRoundRobin": false
                    }
                    """.trimIndent()
            }.andExpect {
                status { isBadRequest() }
            }
    }

    // ========== Helper Methods ==========

    private fun createSchedules(
        teamCount: Int,
        matchCount: Int,
    ): List<LeagueSchedule> {
        val competition = createCompetition()
        val teams = (1L..teamCount.toLong()).map { createTeam("팀$it", it) }

        return (1..matchCount).map { i ->
            val homeTeam = teams[(i - 1) % teamCount]
            val awayTeam = teams[((i - 1) + 1) % teamCount]

            LeagueSchedule.create(
                competition = competition,
                round = (i - 1) / (teamCount / 2) + 1,
                matchNumber = i,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledDate = LocalDate.now().plusWeeks((i - 1).toLong()),
            )
        }
    }

    private fun createCompetition(): Competition {
        val association = Association(name = "서울시야구협회", abbreviation = "서야협", region = "서울")
        val league = League(association = association, name = "서울시 리그", foundedYear = 2020)
        return Competition(
            league = league,
            name = "2024 시즌",
            year = 2024,
            startDate = LocalDate.now().minusDays(30),
            id = 1L,
        )
    }

    private fun createTeam(
        name: String,
        id: Long,
    ): Team {
        val association = Association(name = "서울시야구협회", abbreviation = "서야협", region = "서울")
        val league = League(association = association, name = "서울시 리그", foundedYear = 2020)
        return Team(
            league = league,
            name = name,
            city = "서울",
            foundedYear = 2020,
            id = id,
        )
    }
}
