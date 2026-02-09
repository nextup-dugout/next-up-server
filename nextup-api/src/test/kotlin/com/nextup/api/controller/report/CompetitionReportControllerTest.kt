package com.nextup.api.controller.report

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.service.report.CompetitionReportService
import com.nextup.core.service.report.dto.CompetitionReportDto
import com.nextup.core.service.report.dto.CompetitionSummaryDto
import com.nextup.core.service.report.dto.HighScoringGameDto
import com.nextup.core.service.report.dto.WinStreakDto
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

class CompetitionReportControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var reportService: CompetitionReportService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        reportService = mockk()
        val controller = CompetitionReportController(reportService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @Test
    fun `should return full report successfully`() {
        // given
        val competitionId = 1L
        val summary =
            CompetitionSummaryDto(
                competitionId = competitionId,
                totalGames = 10,
                completedGames = 8,
                totalRuns = 80,
                averageRunsPerGame = 10.0,
                totalHits = 120,
                totalHomeRuns = 15,
                totalStrikeouts = 100,
                highestScoringGame =
                    HighScoringGameDto(
                        gameId = 5L,
                        homeTeamName = "팀A",
                        awayTeamName = "팀B",
                        totalRuns = 20,
                        date = LocalDate.of(2024, 3, 15),
                    ),
                longestWinStreak =
                    WinStreakDto(
                        teamName = "팀A",
                        streakLength = 5,
                    ),
            )

        val report =
            CompetitionReportDto(
                competitionId = competitionId,
                competitionName = "2024 춘계대회",
                season = 1,
                standings = emptyList(),
                summary = summary,
            )

        every { reportService.getReport(competitionId) } returns report

        // when & then
        mockMvc
            .perform(
                get("/api/v1/competitions/{competitionId}/report", competitionId)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.competitionId").value(competitionId))
            .andExpect(jsonPath("$.data.competitionName").value("2024 춘계대회"))
            .andExpect(jsonPath("$.data.season").value(1))
            .andExpect(jsonPath("$.data.summary.totalGames").value(10))
            .andExpect(jsonPath("$.data.summary.completedGames").value(8))
            .andExpect(jsonPath("$.data.summary.totalRuns").value(80))
            .andExpect(jsonPath("$.data.summary.averageRunsPerGame").value(10.0))
            .andExpect(jsonPath("$.data.summary.totalHits").value(120))
            .andExpect(jsonPath("$.data.summary.totalHomeRuns").value(15))
            .andExpect(jsonPath("$.data.summary.totalStrikeouts").value(100))
            .andExpect(jsonPath("$.data.summary.highestScoringGame.gameId").value(5))
            .andExpect(jsonPath("$.data.summary.highestScoringGame.homeTeamName").value("팀A"))
            .andExpect(jsonPath("$.data.summary.highestScoringGame.awayTeamName").value("팀B"))
            .andExpect(jsonPath("$.data.summary.highestScoringGame.totalRuns").value(20))
            .andExpect(jsonPath("$.data.summary.longestWinStreak.teamName").value("팀A"))
            .andExpect(jsonPath("$.data.summary.longestWinStreak.streakLength").value(5))
    }

    @Test
    fun `should return summary only successfully`() {
        // given
        val competitionId = 1L
        val summary =
            CompetitionSummaryDto(
                competitionId = competitionId,
                totalGames = 10,
                completedGames = 8,
                totalRuns = 80,
                averageRunsPerGame = 10.0,
                totalHits = 120,
                totalHomeRuns = 15,
                totalStrikeouts = 100,
                highestScoringGame = null,
                longestWinStreak = null,
            )

        every { reportService.getReportSummary(competitionId) } returns summary

        // when & then
        mockMvc
            .perform(
                get("/api/v1/competitions/{competitionId}/report/summary", competitionId)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.competitionId").value(competitionId))
            .andExpect(jsonPath("$.data.totalGames").value(10))
            .andExpect(jsonPath("$.data.completedGames").value(8))
            .andExpect(jsonPath("$.data.totalRuns").value(80))
            .andExpect(jsonPath("$.data.highestScoringGame").isEmpty)
            .andExpect(jsonPath("$.data.longestWinStreak").isEmpty)
    }

    @Test
    fun `should return 404 when competition not found`() {
        // given
        val competitionId = 999L
        every { reportService.getReport(competitionId) } throws CompetitionNotFoundException(competitionId)

        // when & then
        mockMvc
            .perform(
                get("/api/v1/competitions/{competitionId}/report", competitionId)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("COMPETITION_NOT_FOUND"))
    }

    @Test
    fun `should return 404 when competition not found for summary`() {
        // given
        val competitionId = 999L
        every { reportService.getReportSummary(competitionId) } throws CompetitionNotFoundException(competitionId)

        // when & then
        mockMvc
            .perform(
                get("/api/v1/competitions/{competitionId}/report/summary", competitionId)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("COMPETITION_NOT_FOUND"))
    }
}
