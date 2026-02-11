package com.nextup.api.controller.stats

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.service.stats.MatchupService
import com.nextup.core.service.stats.dto.MatchupDto
import com.nextup.core.service.stats.dto.MatchupHistoryDto
import com.nextup.core.service.stats.dto.MatchupStatsDto
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

@DisplayName("MatchupController 테스트")
class MatchupControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var matchupService: MatchupService

    @BeforeEach
    fun setUp() {
        matchupService = mockk()

        val controller = MatchupController(matchupService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/matchups/pitcher/{pitcherId}/batter/{batterId}")
    inner class GetMatchup {
        @Test
        fun `투수 vs 타자 매치업을 정상적으로 조회한다`() {
            // given
            val pitcherId = 100L
            val batterId = 200L
            val matchupDto =
                MatchupDto(
                    pitcherId = pitcherId,
                    pitcherName = "김투수",
                    batterId = batterId,
                    batterName = "이타자",
                    year = null,
                    stats =
                        MatchupStatsDto(
                            plateAppearances = 15,
                            atBats = 12,
                            hits = 4,
                            doubles = 1,
                            triples = 0,
                            homeRuns = 1,
                            walks = 2,
                            strikeouts = 3,
                            hitByPitch = 1,
                            sacrificeFlies = 0,
                            runsBattedIn = 3,
                            battingAverage = BigDecimal("0.333"),
                            onBasePercentage = BigDecimal("0.467"),
                            sluggingPercentage = BigDecimal("0.583"),
                        ),
                    history =
                        listOf(
                            MatchupHistoryDto(
                                gameId = 1L,
                                gameDate = "2026-01-15",
                                result = "안타",
                                description = "2타수 1안타 (1루타)",
                            ),
                            MatchupHistoryDto(
                                gameId = 2L,
                                gameDate = "2026-01-22",
                                result = "홈런",
                                description = "3타수 1안타 (솔로 홈런)",
                            ),
                        ),
                )

            every { matchupService.getMatchup(pitcherId, batterId, null, null) } returns matchupDto

            // when & then
            mockMvc
                .perform(get("/api/v1/matchups/pitcher/$pitcherId/batter/$batterId"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pitcherId").value(pitcherId))
                .andExpect(jsonPath("$.data.pitcherName").value("김투수"))
                .andExpect(jsonPath("$.data.batterId").value(batterId))
                .andExpect(jsonPath("$.data.batterName").value("이타자"))
                .andExpect(jsonPath("$.data.stats.plateAppearances").value(15))
                .andExpect(jsonPath("$.data.stats.hits").value(4))
                .andExpect(jsonPath("$.data.history").isArray)
                .andExpect(jsonPath("$.data.history[0].gameDate").value("2026-01-15"))
        }

        @Test
        fun `연도 필터로 매치업을 조회한다`() {
            // given
            val pitcherId = 100L
            val batterId = 200L
            val year = 2025
            val matchupDto =
                MatchupDto(
                    pitcherId = pitcherId,
                    pitcherName = "김투수",
                    batterId = batterId,
                    batterName = "이타자",
                    year = year,
                    stats =
                        MatchupStatsDto(
                            plateAppearances = 8,
                            atBats = 7,
                            hits = 2,
                            doubles = 0,
                            triples = 0,
                            homeRuns = 0,
                            walks = 1,
                            strikeouts = 2,
                            hitByPitch = 0,
                            sacrificeFlies = 0,
                            runsBattedIn = 1,
                            battingAverage = BigDecimal("0.286"),
                            onBasePercentage = BigDecimal("0.375"),
                            sluggingPercentage = BigDecimal("0.286"),
                        ),
                    history = emptyList(),
                )

            every { matchupService.getMatchup(pitcherId, batterId, year, null) } returns matchupDto

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/matchups/pitcher/$pitcherId/batter/$batterId")
                        .param("year", year.toString()),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.year").value(year))
        }

        @Test
        fun `매치업 통계를 정확하게 반환한다`() {
            // given
            val pitcherId = 100L
            val batterId = 200L
            val matchupDto =
                MatchupDto(
                    pitcherId = pitcherId,
                    pitcherName = "에이스",
                    batterId = batterId,
                    batterName = "클린업",
                    year = null,
                    stats =
                        MatchupStatsDto(
                            plateAppearances = 20,
                            atBats = 16,
                            hits = 5,
                            doubles = 2,
                            triples = 1,
                            homeRuns = 2,
                            walks = 3,
                            strikeouts = 5,
                            hitByPitch = 1,
                            sacrificeFlies = 0,
                            runsBattedIn = 6,
                            battingAverage = BigDecimal("0.313"),
                            onBasePercentage = BigDecimal("0.450"),
                            sluggingPercentage = BigDecimal("0.750"),
                        ),
                    history = emptyList(),
                )

            every { matchupService.getMatchup(pitcherId, batterId, null, null) } returns matchupDto

            // when & then
            mockMvc
                .perform(get("/api/v1/matchups/pitcher/$pitcherId/batter/$batterId"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.stats.plateAppearances").value(20))
                .andExpect(jsonPath("$.data.stats.atBats").value(16))
                .andExpect(jsonPath("$.data.stats.hits").value(5))
                .andExpect(jsonPath("$.data.stats.doubles").value(2))
                .andExpect(jsonPath("$.data.stats.triples").value(1))
                .andExpect(jsonPath("$.data.stats.homeRuns").value(2))
                .andExpect(jsonPath("$.data.stats.walks").value(3))
                .andExpect(jsonPath("$.data.stats.strikeouts").value(5))
                .andExpect(jsonPath("$.data.stats.runsBattedIn").value(6))
        }

        @Test
        fun `예외 발생 시 에러 응답을 반환한다`() {
            // given
            val pitcherId = 999L
            val batterId = 888L
            every { matchupService.getMatchup(pitcherId, batterId, null, null) } throws
                IllegalArgumentException("선수를 찾을 수 없습니다")

            // when & then
            mockMvc
                .perform(get("/api/v1/matchups/pitcher/$pitcherId/batter/$batterId"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
