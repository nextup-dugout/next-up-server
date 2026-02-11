package com.nextup.api.controller.stats

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.service.stats.IndividualRankingService
import com.nextup.core.service.stats.dto.BattingCategory
import com.nextup.core.service.stats.dto.BattingLeaderDto
import com.nextup.core.service.stats.dto.PitchingCategory
import com.nextup.core.service.stats.dto.PitchingLeaderDto
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

@DisplayName("LeaderboardController 테스트")
class LeaderboardControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var individualRankingService: IndividualRankingService

    private val competitionId = 1L

    @BeforeEach
    fun setUp() {
        individualRankingService = mockk()
        val controller = LeaderboardController(individualRankingService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/competitions/{competitionId}/leaderboard/batting")
    inner class GetBattingLeaders {
        @Test
        fun `should return batting leaders successfully`() {
            // given
            val leaders =
                listOf(
                    BattingLeaderDto(
                        rank = 1,
                        playerId = 10L,
                        playerName = "홍길동",
                        teamName = "타이거즈",
                        value = 0.350,
                        games = 15,
                        plateAppearances = 50,
                    ),
                    BattingLeaderDto(
                        rank = 2,
                        playerId = 20L,
                        playerName = "김철수",
                        teamName = "이글스",
                        value = 0.320,
                        games = 14,
                        plateAppearances = 48,
                    ),
                )
            every {
                individualRankingService.getBattingLeaders(competitionId, BattingCategory.BATTING_AVG, 10)
            } returns leaders

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/leaderboard/batting")
                        .param("category", "BATTING_AVG"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].playerName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].teamName").value("타이거즈"))
                .andExpect(jsonPath("$.data[0].value").value(0.350))
                .andExpect(jsonPath("$.data[1].rank").value(2))
                .andExpect(jsonPath("$.data[1].playerName").value("김철수"))
        }

        @Test
        fun `should return home run leaders`() {
            // given
            val leaders =
                listOf(
                    BattingLeaderDto(
                        rank = 1,
                        playerId = 10L,
                        playerName = "홍길동",
                        teamName = "타이거즈",
                        value = 5.0,
                        games = 15,
                        plateAppearances = 50,
                    ),
                )
            every {
                individualRankingService.getBattingLeaders(competitionId, BattingCategory.HOME_RUNS, 10)
            } returns leaders

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/leaderboard/batting")
                        .param("category", "HOME_RUNS"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].value").value(5.0))
        }

        @Test
        fun `should return empty list when no stats exist`() {
            // given
            every {
                individualRankingService.getBattingLeaders(competitionId, BattingCategory.BATTING_AVG, 10)
            } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/leaderboard/batting")
                        .param("category", "BATTING_AVG"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        fun `should return 404 when competition not found`() {
            // given
            every {
                individualRankingService.getBattingLeaders(999L, BattingCategory.BATTING_AVG, 10)
            } throws CompetitionNotFoundException(999L)

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/competitions/999/leaderboard/batting")
                        .param("category", "BATTING_AVG"),
                )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMPETITION_NOT_FOUND"))
        }

        @Test
        fun `should support custom limit parameter`() {
            // given
            every {
                individualRankingService.getBattingLeaders(competitionId, BattingCategory.RBI, 5)
            } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/leaderboard/batting")
                        .param("category", "RBI")
                        .param("limit", "5"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/competitions/{competitionId}/leaderboard/pitching")
    inner class GetPitchingLeaders {
        @Test
        fun `should return pitching leaders successfully`() {
            // given
            val leaders =
                listOf(
                    PitchingLeaderDto(
                        rank = 1,
                        playerId = 30L,
                        playerName = "박지성",
                        teamName = "라이온즈",
                        value = 2.50,
                        games = 12,
                        inningsPitched = 36.0,
                    ),
                    PitchingLeaderDto(
                        rank = 2,
                        playerId = 40L,
                        playerName = "이승엽",
                        teamName = "베어스",
                        value = 3.10,
                        games = 10,
                        inningsPitched = 30.0,
                    ),
                )
            every {
                individualRankingService.getPitchingLeaders(competitionId, PitchingCategory.ERA, 10)
            } returns leaders

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/leaderboard/pitching")
                        .param("category", "ERA"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].playerName").value("박지성"))
                .andExpect(jsonPath("$.data[0].value").value(2.50))
                .andExpect(jsonPath("$.data[0].inningsPitched").value(36.0))
        }

        @Test
        fun `should return wins leaders`() {
            // given
            val leaders =
                listOf(
                    PitchingLeaderDto(
                        rank = 1,
                        playerId = 30L,
                        playerName = "박지성",
                        teamName = "라이온즈",
                        value = 8.0,
                        games = 15,
                        inningsPitched = 50.0,
                    ),
                )
            every {
                individualRankingService.getPitchingLeaders(competitionId, PitchingCategory.WINS, 10)
            } returns leaders

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/leaderboard/pitching")
                        .param("category", "WINS"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].value").value(8.0))
        }

        @Test
        fun `should return 404 when competition not found for pitching`() {
            // given
            every {
                individualRankingService.getPitchingLeaders(999L, PitchingCategory.ERA, 10)
            } throws CompetitionNotFoundException(999L)

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/competitions/999/leaderboard/pitching")
                        .param("category", "ERA"),
                )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
