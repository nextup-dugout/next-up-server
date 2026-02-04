package com.nextup.api.controller.stats

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.service.stats.TeamStatsService
import com.nextup.core.service.stats.dto.TeamBattingStatsDto
import com.nextup.core.service.stats.dto.TeamPitchingStatsDto
import com.nextup.core.service.stats.dto.TeamRecordDto
import com.nextup.core.service.stats.dto.TeamStatsDto
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

@DisplayName("TeamStatsController 테스트")
class TeamStatsControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var teamStatsService: TeamStatsService

    @BeforeEach
    fun setUp() {
        teamStatsService = mockk()

        val controller = TeamStatsController(teamStatsService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/stats")
    inner class GetTeamStats {
        @Test
        fun `팀 통계를 정상적으로 조회한다`() {
            // given
            val teamId = 1L
            val teamStatsDto =
                TeamStatsDto(
                    teamId = teamId,
                    teamName = "테스트팀",
                    year = 2026,
                    competitionId = 10L,
                    competitionName = "봄시즌리그",
                    record =
                        TeamRecordDto(
                            gamesPlayed = 20,
                            wins = 12,
                            losses = 6,
                            draws = 2,
                            winningPercentage = BigDecimal("0.667"),
                        ),
                    batting =
                        TeamBattingStatsDto(
                            totalAtBats = 700,
                            totalHits = 210,
                            totalHomeRuns = 25,
                            totalRunsBattedIn = 95,
                            totalRuns = 100,
                            teamBattingAverage = BigDecimal("0.300"),
                            teamOnBasePercentage = BigDecimal("0.380"),
                            teamSluggingPercentage = BigDecimal("0.450"),
                        ),
                    pitching =
                        TeamPitchingStatsDto(
                            totalInningsPitchedOuts = 540,
                            inningsPitchedDisplay = "180.0",
                            totalEarnedRuns = 60,
                            totalStrikeouts = 150,
                            totalWalksAllowed = 55,
                            teamEra = BigDecimal("3.00"),
                            teamWhip = BigDecimal("1.20"),
                        ),
                )

            every { teamStatsService.getTeamStats(teamId, null, null) } returns teamStatsDto

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/$teamId/stats"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(teamId))
                .andExpect(jsonPath("$.data.teamName").value("테스트팀"))
                .andExpect(jsonPath("$.data.record.gamesPlayed").value(20))
                .andExpect(jsonPath("$.data.record.wins").value(12))
                .andExpect(jsonPath("$.data.batting.totalHits").value(210))
                .andExpect(jsonPath("$.data.pitching.inningsPitchedDisplay").value("180.0"))
        }

        @Test
        fun `연도 필터로 팀 통계를 조회한다`() {
            // given
            val teamId = 1L
            val year = 2025
            val teamStatsDto =
                TeamStatsDto(
                    teamId = teamId,
                    teamName = "테스트팀",
                    year = year,
                    competitionId = null,
                    competitionName = null,
                    record =
                        TeamRecordDto(
                            gamesPlayed = 15,
                            wins = 8,
                            losses = 5,
                            draws = 2,
                            winningPercentage = BigDecimal("0.615"),
                        ),
                    batting =
                        TeamBattingStatsDto(
                            totalAtBats = 500,
                            totalHits = 140,
                            totalHomeRuns = 15,
                            totalRunsBattedIn = 60,
                            totalRuns = 70,
                            teamBattingAverage = BigDecimal("0.280"),
                            teamOnBasePercentage = BigDecimal("0.350"),
                            teamSluggingPercentage = BigDecimal("0.420"),
                        ),
                    pitching =
                        TeamPitchingStatsDto(
                            totalInningsPitchedOuts = 405,
                            inningsPitchedDisplay = "135.0",
                            totalEarnedRuns = 50,
                            totalStrikeouts = 100,
                            totalWalksAllowed = 45,
                            teamEra = BigDecimal("3.33"),
                            teamWhip = BigDecimal("1.30"),
                        ),
                )

            every { teamStatsService.getTeamStats(teamId, year, null) } returns teamStatsDto

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/teams/$teamId/stats")
                        .param("year", year.toString()),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.year").value(year))
        }

        @Test
        fun `대회 ID로 팀 통계를 조회한다`() {
            // given
            val teamId = 1L
            val competitionId = 5L
            val teamStatsDto =
                TeamStatsDto(
                    teamId = teamId,
                    teamName = "테스트팀",
                    year = null,
                    competitionId = competitionId,
                    competitionName = "가을리그",
                    record =
                        TeamRecordDto(
                            gamesPlayed = 10,
                            wins = 7,
                            losses = 2,
                            draws = 1,
                            winningPercentage = BigDecimal("0.778"),
                        ),
                    batting =
                        TeamBattingStatsDto(
                            totalAtBats = 350,
                            totalHits = 100,
                            totalHomeRuns = 12,
                            totalRunsBattedIn = 45,
                            totalRuns = 50,
                            teamBattingAverage = BigDecimal("0.286"),
                            teamOnBasePercentage = BigDecimal("0.360"),
                            teamSluggingPercentage = BigDecimal("0.430"),
                        ),
                    pitching =
                        TeamPitchingStatsDto(
                            totalInningsPitchedOuts = 270,
                            inningsPitchedDisplay = "90.0",
                            totalEarnedRuns = 25,
                            totalStrikeouts = 80,
                            totalWalksAllowed = 30,
                            teamEra = BigDecimal("2.50"),
                            teamWhip = BigDecimal("1.10"),
                        ),
                )

            every { teamStatsService.getTeamStats(teamId, null, competitionId) } returns teamStatsDto

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/teams/$teamId/stats")
                        .param("competitionId", competitionId.toString()),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.competitionId").value(competitionId))
                .andExpect(jsonPath("$.data.competitionName").value("가을리그"))
        }

        @Test
        fun `예외 발생 시 에러 응답을 반환한다`() {
            // given
            val teamId = 999L
            every { teamStatsService.getTeamStats(teamId, null, null) } throws
                IllegalArgumentException("팀을 찾을 수 없습니다")

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/$teamId/stats"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
