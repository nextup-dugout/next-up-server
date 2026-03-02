package com.nextup.api.controller.stats

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CareerFieldingStatsNotFoundException
import com.nextup.common.exception.SeasonFieldingStatsNotFoundException
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.service.stats.PlayerFieldingStatsService
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

@DisplayName("PlayerFieldingStatsController 테스트")
class PlayerFieldingStatsControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var playerFieldingStatsService: PlayerFieldingStatsService

    private val playerId = 1L
    private val year = 2026

    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.SHORTSTOP,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = playerId,
        )

    @BeforeEach
    fun setUp() {
        playerFieldingStatsService = mockk()

        val controller = PlayerFieldingStatsController(playerFieldingStatsService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/stats/fielding/season/{year}")
    inner class GetSeasonFieldingStats {
        @Test
        fun `시즌 수비 통계를 성공적으로 조회한다`() {
            // given
            val stats =
                SeasonFieldingStats.create(testPlayer, year).apply {
                    setStatsDirectly(
                        gamesPlayed = 15,
                        putOuts = 30,
                        assists = 20,
                        errors = 3,
                        doublePlays = 5,
                        passedBalls = 0,
                    )
                }

            every { playerFieldingStatsService.getSeasonFieldingStats(playerId, year) } returns stats

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/stats/fielding/season/$year"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(playerId))
                .andExpect(jsonPath("$.data.year").value(year))
                .andExpect(jsonPath("$.data.gamesPlayed").value(15))
                .andExpect(jsonPath("$.data.putOuts").value(30))
                .andExpect(jsonPath("$.data.assists").value(20))
                .andExpect(jsonPath("$.data.errors").value(3))
                .andExpect(jsonPath("$.data.doublePlays").value(5))
                .andExpect(jsonPath("$.data.passedBalls").value(0))
                .andExpect(jsonPath("$.data.totalChances").value(53))
        }

        @Test
        fun `시즌 수비 통계가 없으면 404를 반환한다`() {
            // given
            every { playerFieldingStatsService.getSeasonFieldingStats(playerId, year) } throws
                SeasonFieldingStatsNotFoundException(playerId, year)

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/stats/fielding/season/$year"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/stats/fielding/career")
    inner class GetCareerFieldingStats {
        @Test
        fun `통산 수비 통계를 성공적으로 조회한다`() {
            // given
            val stats =
                CareerFieldingStats.create(testPlayer).apply {
                    setCareerStatsDirectly(
                        seasonsPlayed = 3,
                        gamesPlayed = 45,
                        putOuts = 90,
                        assists = 60,
                        errors = 8,
                        doublePlays = 12,
                        passedBalls = 2,
                    )
                }

            every { playerFieldingStatsService.getCareerFieldingStats(playerId) } returns stats

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/stats/fielding/career"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(playerId))
                .andExpect(jsonPath("$.data.seasonsPlayed").value(3))
                .andExpect(jsonPath("$.data.gamesPlayed").value(45))
                .andExpect(jsonPath("$.data.putOuts").value(90))
                .andExpect(jsonPath("$.data.assists").value(60))
                .andExpect(jsonPath("$.data.errors").value(8))
                .andExpect(jsonPath("$.data.doublePlays").value(12))
                .andExpect(jsonPath("$.data.passedBalls").value(2))
                .andExpect(jsonPath("$.data.totalChances").value(158))
        }

        @Test
        fun `통산 수비 통계가 없으면 404를 반환한다`() {
            // given
            every { playerFieldingStatsService.getCareerFieldingStats(playerId) } throws
                CareerFieldingStatsNotFoundException(playerId)

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/stats/fielding/career"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/stats/fielding/seasons")
    inner class GetAllSeasonFieldingStats {
        @Test
        fun `모든 시즌 수비 통계를 성공적으로 조회한다`() {
            // given
            val stats2025 =
                SeasonFieldingStats.create(testPlayer, 2025).apply {
                    setStatsDirectly(gamesPlayed = 10, putOuts = 20, assists = 10, errors = 2)
                }
            val stats2026 =
                SeasonFieldingStats.create(testPlayer, 2026).apply {
                    setStatsDirectly(gamesPlayed = 15, putOuts = 30, assists = 20, errors = 3)
                }

            every { playerFieldingStatsService.getAllSeasonFieldingStats(playerId) } returns
                listOf(stats2025, stats2026)

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/stats/fielding/seasons"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].year").value(2025))
                .andExpect(jsonPath("$.data[0].gamesPlayed").value(10))
                .andExpect(jsonPath("$.data[1].year").value(2026))
                .andExpect(jsonPath("$.data[1].gamesPlayed").value(15))
        }

        @Test
        fun `시즌 수비 통계가 없으면 빈 리스트를 반환한다`() {
            // given
            every { playerFieldingStatsService.getAllSeasonFieldingStats(playerId) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/stats/fielding/seasons"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    // Helper methods

    private fun SeasonFieldingStats.setStatsDirectly(
        gamesPlayed: Int = 0,
        putOuts: Int = 0,
        assists: Int = 0,
        errors: Int = 0,
        doublePlays: Int = 0,
        passedBalls: Int = 0,
    ) {
        val clazz = SeasonFieldingStats::class.java
        setField(clazz, this, "gamesPlayed", gamesPlayed)
        setField(clazz, this, "putOuts", putOuts)
        setField(clazz, this, "assists", assists)
        setField(clazz, this, "errors", errors)
        setField(clazz, this, "doublePlays", doublePlays)
        setField(clazz, this, "passedBalls", passedBalls)
    }

    private fun CareerFieldingStats.setCareerStatsDirectly(
        seasonsPlayed: Int = 0,
        gamesPlayed: Int = 0,
        putOuts: Int = 0,
        assists: Int = 0,
        errors: Int = 0,
        doublePlays: Int = 0,
        passedBalls: Int = 0,
    ) {
        val clazz = CareerFieldingStats::class.java
        setField(clazz, this, "seasonsPlayed", seasonsPlayed)
        setField(clazz, this, "gamesPlayed", gamesPlayed)
        setField(clazz, this, "putOuts", putOuts)
        setField(clazz, this, "assists", assists)
        setField(clazz, this, "errors", errors)
        setField(clazz, this, "doublePlays", doublePlays)
        setField(clazz, this, "passedBalls", passedBalls)
    }

    private fun setField(
        clazz: Class<*>,
        obj: Any,
        fieldName: String,
        value: Any?,
    ) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
