package com.nextup.api.controller.team

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.service.team.TeamDashboardService
import com.nextup.core.service.team.dto.GameSummaryForDashboardDto
import com.nextup.core.service.team.dto.PollSummaryDto
import com.nextup.core.service.team.dto.StandingEntryDto
import com.nextup.core.service.team.dto.TeamDashboardDto
import com.nextup.core.service.team.dto.TeamStatsSummaryDto
import com.nextup.core.service.team.dto.TeamSummaryDto
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

@DisplayName("TeamDashboardController")
class TeamDashboardControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var teamDashboardService: TeamDashboardService
    private lateinit var controller: TeamDashboardController

    @BeforeEach
    fun setUp() {
        teamDashboardService = mockk()
        controller = TeamDashboardController(teamDashboardService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    private fun buildDashboardDto(
        teamId: Long = 1L,
        memberCount: Int = 15,
        nextGame: GameSummaryForDashboardDto? = null,
        recentResults: List<GameSummaryForDashboardDto> = emptyList(),
        standing: StandingEntryDto? = null,
        activePoll: PollSummaryDto? = null,
        teamStats: TeamStatsSummaryDto? = null,
    ) = TeamDashboardDto(
        team =
            TeamSummaryDto(
                teamId = teamId,
                name = "테스트팀",
                city = "서울",
                abbreviation = "테스",
                logoUrl = null,
                leagueName = "서울 리그",
                foundedYear = 2020,
            ),
        memberCount = memberCount,
        nextGame = nextGame,
        recentResults = recentResults,
        standing = standing,
        activePoll = activePoll,
        teamStats = teamStats,
    )

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/dashboard")
    inner class GetTeamDashboard {
        @Test
        fun `팀 대시보드를 조회할 수 있다`() {
            // given
            val dto = buildDashboardDto(teamId = 1L, memberCount = 15)
            every { teamDashboardService.getTeamDashboard(1L) } returns dto

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/dashboard"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.team.teamId").value(1))
                .andExpect(jsonPath("$.data.team.name").value("테스트팀"))
                .andExpect(jsonPath("$.data.team.city").value("서울"))
                .andExpect(jsonPath("$.data.memberCount").value(15))
        }

        @Test
        fun `다음 경기가 있을 때 nextGame을 포함한다`() {
            // given
            val nextGame =
                GameSummaryForDashboardDto(
                    gameId = 10L,
                    competitionId = 100L,
                    competitionName = "봄리그",
                    homeTeamId = 1L,
                    homeTeamName = "테스트팀",
                    awayTeamId = 2L,
                    awayTeamName = "상대팀",
                    scheduledAt = LocalDateTime.now().plusDays(3),
                    status = GameStatus.SCHEDULED,
                    homeScore = 0,
                    awayScore = 0,
                    location = "서울",
                    fieldName = "잠실구장",
                )
            val dto = buildDashboardDto(nextGame = nextGame)
            every { teamDashboardService.getTeamDashboard(1L) } returns dto

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/dashboard"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nextGame.gameId").value(10))
                .andExpect(jsonPath("$.data.nextGame.competitionName").value("봄리그"))
                .andExpect(jsonPath("$.data.nextGame.homeTeam.teamName").value("테스트팀"))
                .andExpect(jsonPath("$.data.nextGame.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.nextGame.fieldName").value("잠실구장"))
        }

        @Test
        fun `다음 경기가 없을 때 nextGame은 null이다`() {
            // given
            val dto = buildDashboardDto(nextGame = null)
            every { teamDashboardService.getTeamDashboard(1L) } returns dto

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/dashboard"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nextGame").doesNotExist())
        }

        @Test
        fun `최근 결과가 있을 때 recentResults를 포함한다`() {
            // given
            val recentGame =
                GameSummaryForDashboardDto(
                    gameId = 5L,
                    competitionId = 100L,
                    competitionName = "봄리그",
                    homeTeamId = 1L,
                    homeTeamName = "테스트팀",
                    awayTeamId = 2L,
                    awayTeamName = "상대팀",
                    scheduledAt = LocalDateTime.now().minusDays(3),
                    status = GameStatus.FINISHED,
                    homeScore = 5,
                    awayScore = 3,
                    location = "서울",
                    fieldName = null,
                )
            val dto = buildDashboardDto(recentResults = listOf(recentGame))
            every { teamDashboardService.getTeamDashboard(1L) } returns dto

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/dashboard"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recentResults").isArray)
                .andExpect(jsonPath("$.data.recentResults.length()").value(1))
                .andExpect(jsonPath("$.data.recentResults[0].gameId").value(5))
                .andExpect(jsonPath("$.data.recentResults[0].homeTeam.score").value(5))
                .andExpect(jsonPath("$.data.recentResults[0].awayTeam.score").value(3))
        }

        @Test
        fun `순위가 있을 때 standing을 포함한다`() {
            // given
            val standing =
                StandingEntryDto(
                    rank = 2,
                    teamId = 1L,
                    teamName = "테스트팀",
                    competitionId = 100L,
                    competitionName = "봄리그",
                    gamesPlayed = 10,
                    wins = 7,
                    losses = 2,
                    draws = 1,
                    winningPercentage = BigDecimal("0.778"),
                    gamesBehind = BigDecimal("1.0"),
                )
            val dto = buildDashboardDto(standing = standing)
            every { teamDashboardService.getTeamDashboard(1L) } returns dto

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/dashboard"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.standing.rank").value(2))
                .andExpect(jsonPath("$.data.standing.wins").value(7))
                .andExpect(jsonPath("$.data.standing.losses").value(2))
                .andExpect(jsonPath("$.data.standing.competitionName").value("봄리그"))
        }

        @Test
        fun `진행 중인 투표가 있을 때 activePoll을 포함한다`() {
            // given
            val poll =
                PollSummaryDto(
                    pollId = 20L,
                    title = "이번 주 경기 참석 여부",
                    eventDate = LocalDateTime.now().plusDays(5),
                    deadline = LocalDateTime.now().plusDays(3),
                    status = PollStatus.OPEN,
                )
            val dto = buildDashboardDto(activePoll = poll)
            every { teamDashboardService.getTeamDashboard(1L) } returns dto

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/dashboard"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activePoll.pollId").value(20))
                .andExpect(jsonPath("$.data.activePoll.title").value("이번 주 경기 참석 여부"))
                .andExpect(jsonPath("$.data.activePoll.status").value("OPEN"))
        }

        @Test
        fun `팀 통계가 있을 때 teamStats를 포함한다`() {
            // given
            val stats =
                TeamStatsSummaryDto(
                    gamesPlayed = 10,
                    wins = 7,
                    losses = 2,
                    draws = 1,
                    winningPercentage = BigDecimal("0.778"),
                    teamBattingAverage = BigDecimal("0.000"),
                    teamEra = BigDecimal("0.00"),
                )
            val dto = buildDashboardDto(teamStats = stats)
            every { teamDashboardService.getTeamDashboard(1L) } returns dto

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/dashboard"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamStats.gamesPlayed").value(10))
                .andExpect(jsonPath("$.data.teamStats.wins").value(7))
                .andExpect(jsonPath("$.data.teamStats.losses").value(2))
        }

        @Test
        fun `팀이 없을 때 404를 반환한다`() {
            // given
            every { teamDashboardService.getTeamDashboard(999L) } throws TeamNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/999/dashboard"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TEAM_NOT_FOUND"))
        }
    }
}
