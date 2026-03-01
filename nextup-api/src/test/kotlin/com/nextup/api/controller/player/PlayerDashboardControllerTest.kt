package com.nextup.api.controller.player

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.api.mapper.player.toResponse
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.domain.player.PlayerTeamStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.service.player.PlayerDashboardService
import com.nextup.core.service.player.dto.PlayerDashboardDto
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
import java.time.LocalDate

@DisplayName("PlayerDashboardController")
class PlayerDashboardControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var playerDashboardService: PlayerDashboardService

    @BeforeEach
    fun setUp() {
        playerDashboardService = mockk()
        val controller = PlayerDashboardController(playerDashboardService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/dashboard")
    inner class GetPlayerDashboard {
        @Test
        fun `선수 대시보드를 조회한다`() {
            // given
            val playerId = 1L
            val player =
                Player(
                    id = playerId,
                    name = "홍길동",
                    primaryPosition = Position.STARTING_PITCHER,
                )
            val dashboard =
                PlayerDashboardDto(
                    player = player,
                    currentHistory = null,
                    seasonBattingStats = null,
                    seasonPitchingStats = null,
                    careerBattingStats = null,
                    careerPitchingStats = null,
                    recentBattingForm = null,
                    recentPitchingForm = null,
                    teamHistory = emptyList(),
                )

            every { playerDashboardService.getPlayerDashboard(playerId) } returns dashboard

            // when & then
            mockMvc
                .perform(get("/api/v1/players/{playerId}/dashboard", playerId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.id").value(playerId))
                .andExpect(jsonPath("$.data.profile.name").value("홍길동"))
                .andExpect(jsonPath("$.data.teamHistory").isArray)
        }

        @Test
        fun `현재 팀이 있는 선수의 대시보드를 조회한다`() {
            // given
            val playerId = 1L
            val player =
                Player(
                    id = playerId,
                    name = "박선수",
                    primaryPosition = Position.LEFT_FIELD,
                )
            val team = mockk<Team>()
            every { team.id } returns 10L
            every { team.name } returns "Tigers"

            val currentHistory =
                PlayerTeamHistory(
                    player = player,
                    team = team,
                    startDate = LocalDate.of(2024, 1, 1),
                    position = Position.LEFT_FIELD,
                    uniformNumber = 7,
                    status = PlayerTeamStatus.ACTIVE,
                )
            val dashboard =
                PlayerDashboardDto(
                    player = player,
                    currentHistory = currentHistory,
                    seasonBattingStats = null,
                    seasonPitchingStats = null,
                    careerBattingStats = null,
                    careerPitchingStats = null,
                    recentBattingForm = null,
                    recentPitchingForm = null,
                    teamHistory = listOf(currentHistory),
                )

            every { playerDashboardService.getPlayerDashboard(playerId) } returns dashboard

            // when & then
            mockMvc
                .perform(get("/api/v1/players/{playerId}/dashboard", playerId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.id").value(playerId))
                .andExpect(jsonPath("$.data.profile.name").value("박선수"))
                .andExpect(jsonPath("$.data.profile.backNumber").value(7))
                .andExpect(jsonPath("$.data.currentTeam.teamId").value(10))
                .andExpect(jsonPath("$.data.currentTeam.teamName").value("Tigers"))
                .andExpect(jsonPath("$.data.teamHistory").isArray)
                .andExpect(jsonPath("$.data.teamHistory.length()").value(1))
        }

        @Test
        fun `선수가 존재하지 않으면 404 응답`() {
            // given
            val playerId = 999L
            every { playerDashboardService.getPlayerDashboard(playerId) } throws
                PlayerNotFoundException(playerId)

            // when & then
            mockMvc
                .perform(get("/api/v1/players/{playerId}/dashboard", playerId))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PLAYER_NOT_FOUND"))
        }
    }
}
