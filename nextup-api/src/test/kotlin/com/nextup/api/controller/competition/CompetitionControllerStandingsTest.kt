package com.nextup.api.controller.competition

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.service.competition.CompetitionService
import com.nextup.core.service.standings.StandingsService
import com.nextup.core.service.standings.dto.StandingsDto
import com.nextup.core.service.standings.dto.TeamStandingDto
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("CompetitionController - Standings")
class CompetitionControllerStandingsTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var competitionService: CompetitionService
    private lateinit var standingsService: StandingsService

    @BeforeEach
    fun setUp() {
        competitionService = mockk()
        standingsService = mockk()

        val controller = CompetitionController(competitionService, standingsService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/competitions/{id}/standings")
    inner class GetStandings {
        @Test
        fun `GET competitions id standings 정상 조회`() {
            // given
            val competitionId = 1L
            val standingsDto =
                StandingsDto(
                    competitionId = competitionId,
                    competitionName = "2025 춘계대회",
                    totalGamesPerTeam = 10,
                    standings =
                        listOf(
                            TeamStandingDto(
                                rank = 1,
                                teamId = 1L,
                                teamName = "Tigers",
                                gamesPlayed = 5,
                                remainingGames = 5,
                                wins = 4,
                                losses = 1,
                                draws = 0,
                                winningPercentage = BigDecimal("0.800"),
                                gamesBehind = BigDecimal.ZERO,
                                runsScored = 25,
                                runsAllowed = 15,
                                runDifferential = 10,
                            ),
                            TeamStandingDto(
                                rank = 2,
                                teamId = 2L,
                                teamName = "Lions",
                                gamesPlayed = 5,
                                remainingGames = 5,
                                wins = 3,
                                losses = 2,
                                draws = 0,
                                winningPercentage = BigDecimal("0.600"),
                                gamesBehind = BigDecimal("1.0"),
                                runsScored = 20,
                                runsAllowed = 18,
                                runDifferential = 2,
                            ),
                            TeamStandingDto(
                                rank = 3,
                                teamId = 3L,
                                teamName = "Bears",
                                gamesPlayed = 5,
                                remainingGames = 5,
                                wins = 1,
                                losses = 4,
                                draws = 0,
                                winningPercentage = BigDecimal("0.200"),
                                gamesBehind = BigDecimal("3.0"),
                                runsScored = 12,
                                runsAllowed = 28,
                                runDifferential = -16,
                            ),
                        ),
                    lastUpdated = LocalDateTime.of(2025, 3, 15, 10, 30),
                )

            every { standingsService.getStandings(competitionId) } returns standingsDto

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/$competitionId/standings"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.competitionId").value(competitionId))
                .andExpect(jsonPath("$.data.competitionName").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data.totalGamesPerTeam").value(10))
                .andExpect(jsonPath("$.data.standings.length()").value(3))
                // 1위 Tigers
                .andExpect(jsonPath("$.data.standings[0].rank").value(1))
                .andExpect(jsonPath("$.data.standings[0].teamId").value(1))
                .andExpect(jsonPath("$.data.standings[0].teamName").value("Tigers"))
                .andExpect(jsonPath("$.data.standings[0].gamesPlayed").value(5))
                .andExpect(jsonPath("$.data.standings[0].remainingGames").value(5))
                .andExpect(jsonPath("$.data.standings[0].wins").value(4))
                .andExpect(jsonPath("$.data.standings[0].losses").value(1))
                .andExpect(jsonPath("$.data.standings[0].draws").value(0))
                .andExpect(jsonPath("$.data.standings[0].winningPercentage").value(0.800))
                .andExpect(jsonPath("$.data.standings[0].gamesBehind").value(0.0))
                .andExpect(jsonPath("$.data.standings[0].runsScored").value(25))
                .andExpect(jsonPath("$.data.standings[0].runsAllowed").value(15))
                .andExpect(jsonPath("$.data.standings[0].runDifferential").value(10))
                // 2위 Lions
                .andExpect(jsonPath("$.data.standings[1].rank").value(2))
                .andExpect(jsonPath("$.data.standings[1].teamName").value("Lions"))
                .andExpect(jsonPath("$.data.standings[1].gamesBehind").value(1.0))
                // 3위 Bears
                .andExpect(jsonPath("$.data.standings[2].rank").value(3))
                .andExpect(jsonPath("$.data.standings[2].teamName").value("Bears"))
                .andExpect(jsonPath("$.data.standings[2].gamesBehind").value(3.0))
        }

        @Test
        fun `대회가 없으면 404 반환`() {
            // given
            val competitionId = 999L
            every { standingsService.getStandings(competitionId) } throws
                CompetitionNotFoundException(competitionId)

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/$competitionId/standings"))
                .andExpect(status().isNotFound)
        }

        @Test
        fun `경기가 없는 대회는 빈 순위표 반환`() {
            // given
            val competitionId = 1L
            val standingsDto =
                StandingsDto(
                    competitionId = competitionId,
                    competitionName = "2025 춘계대회",
                    totalGamesPerTeam = 0,
                    standings = emptyList(),
                    lastUpdated = LocalDateTime.of(2025, 3, 1, 0, 0),
                )

            every { standingsService.getStandings(competitionId) } returns standingsDto

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/$competitionId/standings"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.competitionId").value(competitionId))
                .andExpect(jsonPath("$.data.standings").isEmpty)
                .andExpect(jsonPath("$.data.totalGamesPerTeam").value(0))
        }

        @Test
        fun `무승부가 포함된 순위표 조회`() {
            // given
            val competitionId = 1L
            val standingsDto =
                StandingsDto(
                    competitionId = competitionId,
                    competitionName = "2025 춘계대회",
                    totalGamesPerTeam = 5,
                    standings =
                        listOf(
                            TeamStandingDto(
                                rank = 1,
                                teamId = 1L,
                                teamName = "Tigers",
                                gamesPlayed = 3,
                                remainingGames = 2,
                                wins = 2,
                                losses = 0,
                                draws = 1,
                                winningPercentage = BigDecimal("0.833"),
                                gamesBehind = BigDecimal.ZERO,
                                runsScored = 15,
                                runsAllowed = 10,
                                runDifferential = 5,
                            ),
                        ),
                    lastUpdated = LocalDateTime.now(),
                )

            every { standingsService.getStandings(competitionId) } returns standingsDto

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/$competitionId/standings"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.standings[0].wins").value(2))
                .andExpect(jsonPath("$.data.standings[0].losses").value(0))
                .andExpect(jsonPath("$.data.standings[0].draws").value(1))
                .andExpect(jsonPath("$.data.standings[0].winningPercentage").value(0.833))
        }

        @Test
        fun `동률팀이 있는 순위표 조회`() {
            // given
            val competitionId = 1L
            val standingsDto =
                StandingsDto(
                    competitionId = competitionId,
                    competitionName = "2025 춘계대회",
                    totalGamesPerTeam = 4,
                    standings =
                        listOf(
                            TeamStandingDto(
                                rank = 1,
                                teamId = 1L,
                                teamName = "Tigers",
                                gamesPlayed = 2,
                                remainingGames = 2,
                                wins = 1,
                                losses = 1,
                                draws = 0,
                                winningPercentage = BigDecimal("0.500"),
                                gamesBehind = BigDecimal.ZERO,
                                runsScored = 10,
                                runsAllowed = 8,
                                runDifferential = 2,
                            ),
                            TeamStandingDto(
                                rank = 2,
                                teamId = 2L,
                                teamName = "Lions",
                                gamesPlayed = 2,
                                remainingGames = 2,
                                wins = 1,
                                losses = 1,
                                draws = 0,
                                winningPercentage = BigDecimal("0.500"),
                                gamesBehind = BigDecimal.ZERO,
                                runsScored = 8,
                                runsAllowed = 10,
                                runDifferential = -2,
                            ),
                        ),
                    lastUpdated = LocalDateTime.now(),
                )

            every { standingsService.getStandings(competitionId) } returns standingsDto

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/$competitionId/standings"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.standings[0].winningPercentage").value(0.500))
                .andExpect(jsonPath("$.data.standings[0].runDifferential").value(2))
                .andExpect(jsonPath("$.data.standings[1].winningPercentage").value(0.500))
                .andExpect(jsonPath("$.data.standings[1].runDifferential").value(-2))
        }
    }
}
