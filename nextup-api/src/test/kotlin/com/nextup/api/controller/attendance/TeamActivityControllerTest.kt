package com.nextup.api.controller.attendance

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.service.attendance.ActivityService
import com.nextup.core.service.attendance.PlayerParticipationRate
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

@DisplayName("TeamActivityController 테스트")
class TeamActivityControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var activityService: ActivityService

    @BeforeEach
    fun setUp() {
        activityService = mockk()
        val controller = TeamActivityController(activityService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/activity")
    inner class ListParticipationRates {
        @Test
        fun `팀의 모든 멤버별 경기참여율을 조회할 수 있다`() {
            // given
            val rates =
                listOf(
                    PlayerParticipationRate(
                        playerId = 10L,
                        playerName = "홍길동",
                        gamesPlayed = 8,
                        totalTeamGames = 10,
                        participationRate = BigDecimal("80.00"),
                    ),
                    PlayerParticipationRate(
                        playerId = 20L,
                        playerName = "김철수",
                        gamesPlayed = 5,
                        totalTeamGames = 10,
                        participationRate = BigDecimal("50.00"),
                    ),
                )
            every { activityService.listGameParticipationRates(1L) } returns rates

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/activity"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].playerId").value(10))
                .andExpect(jsonPath("$.data[0].playerName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].gamesPlayed").value(8))
                .andExpect(jsonPath("$.data[0].totalTeamGames").value(10))
        }

        @Test
        fun `팀이 존재하지 않으면 404를 반환한다`() {
            // given
            every {
                activityService.listGameParticipationRates(999L)
            } throws TeamNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/999/activity"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun `멤버가 없으면 빈 목록을 반환한다`() {
            // given
            every { activityService.listGameParticipationRates(1L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/activity"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/activity/players/{playerId}")
    inner class GetPlayerParticipationRate {
        @Test
        fun `특정 선수의 경기참여율을 조회할 수 있다`() {
            // given
            every {
                activityService.getGameParticipationRate(1L, 10L)
            } returns BigDecimal("75.00")

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/activity/players/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(10))
        }

        @Test
        fun `팀이 존재하지 않으면 404를 반환한다`() {
            // given
            every {
                activityService.getGameParticipationRate(999L, 10L)
            } throws TeamNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/999/activity/players/10"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
