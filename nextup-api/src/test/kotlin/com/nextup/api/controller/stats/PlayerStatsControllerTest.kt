package com.nextup.api.controller.stats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.service.stats.PlayerStatsService
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

@DisplayName("PlayerStatsController 테스트")
class PlayerStatsControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var playerStatsService: PlayerStatsService
    private lateinit var objectMapper: ObjectMapper

    private val playerId = 1L
    private val year = 2024

    private val testPlayer = Player(
        name = "홍길동",
        primaryPosition = Position.CATCHER,
        throwingHand = ThrowingHand.RIGHT,
        battingHand = BattingHand.RIGHT,
        id = playerId
    )

    @BeforeEach
    fun setUp() {
        playerStatsService = mockk()
        objectMapper = jacksonObjectMapper()

        val controller = PlayerStatsController(playerStatsService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/stats/batting/season/{year}")
    inner class GetSeasonBattingStats {

        @Test
        fun `should return season batting stats successfully`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, year).apply {
                setStatsDirectly(
                    gamesPlayed = 10,
                    pa = 45,
                    atBats = 40,
                    hits = 12,
                    doubles = 2,
                    homeRuns = 1,
                    runs = 5,
                    rbi = 8
                )
            }

            every { playerStatsService.getSeasonBattingStats(playerId, year) } returns stats

            // when & then
            mockMvc.perform(get("/api/v1/players/$playerId/stats/batting/season/$year"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(playerId))
                .andExpect(jsonPath("$.data.year").value(year))
                .andExpect(jsonPath("$.data.gamesPlayed").value(10))
                .andExpect(jsonPath("$.data.atBats").value(40))
                .andExpect(jsonPath("$.data.hits").value(12))
                .andExpect(jsonPath("$.data.homeRuns").value(1))
                .andExpect(jsonPath("$.data.battingAverage").value("0.300"))
        }

        @Test
        fun `should return 500 when season batting stats not found`() {
            // given
            every { playerStatsService.getSeasonBattingStats(playerId, year) } throws
                IllegalArgumentException("선수 ID $playerId 의 ${year}년도 타격 통계가 존재하지 않습니다.")

            // when & then
            mockMvc.perform(get("/api/v1/players/$playerId/stats/batting/season/$year"))
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/stats/pitching/season/{year}")
    inner class GetSeasonPitchingStats {

        @Test
        fun `should return season pitching stats successfully`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, year).apply {
                setStatsDirectly(
                    gamesPlayed = 10,
                    gamesStarted = 8,
                    inningsPitchedOuts = 60, // 20 이닝
                    wins = 5,
                    losses = 3,
                    earnedRuns = 15,
                    strikeouts = 40
                )
            }

            every { playerStatsService.getSeasonPitchingStats(playerId, year) } returns stats

            // when & then
            mockMvc.perform(get("/api/v1/players/$playerId/stats/pitching/season/$year"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(playerId))
                .andExpect(jsonPath("$.data.year").value(year))
                .andExpect(jsonPath("$.data.gamesPlayed").value(10))
                .andExpect(jsonPath("$.data.gamesStarted").value(8))
                .andExpect(jsonPath("$.data.wins").value(5))
                .andExpect(jsonPath("$.data.losses").value(3))
                .andExpect(jsonPath("$.data.inningsPitchedDisplay").value("20.0"))
        }

        @Test
        fun `should return 500 when season pitching stats not found`() {
            // given
            every { playerStatsService.getSeasonPitchingStats(playerId, year) } throws
                IllegalArgumentException("선수 ID $playerId 의 ${year}년도 투수 통계가 존재하지 않습니다.")

            // when & then
            mockMvc.perform(get("/api/v1/players/$playerId/stats/pitching/season/$year"))
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/stats/batting/seasons")
    inner class GetAllSeasonBattingStats {

        @Test
        fun `should return all season batting stats successfully`() {
            // given
            val stats2023 = SeasonBattingStats.create(testPlayer, 2023).apply {
                setStatsDirectly(gamesPlayed = 8, atBats = 30, hits = 9)
            }
            val stats2024 = SeasonBattingStats.create(testPlayer, 2024).apply {
                setStatsDirectly(gamesPlayed = 10, atBats = 40, hits = 12)
            }

            every { playerStatsService.getAllSeasonBattingStats(playerId) } returns
                listOf(stats2023, stats2024)

            // when & then
            mockMvc.perform(get("/api/v1/players/$playerId/stats/batting/seasons"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].year").value(2023))
                .andExpect(jsonPath("$.data[0].hits").value(9))
                .andExpect(jsonPath("$.data[1].year").value(2024))
                .andExpect(jsonPath("$.data[1].hits").value(12))
        }

        @Test
        fun `should return empty list when no batting stats exist`() {
            // given
            every { playerStatsService.getAllSeasonBattingStats(playerId) } returns emptyList()

            // when & then
            mockMvc.perform(get("/api/v1/players/$playerId/stats/batting/seasons"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/stats/pitching/seasons")
    inner class GetAllSeasonPitchingStats {

        @Test
        fun `should return all season pitching stats successfully`() {
            // given
            val stats2023 = SeasonPitchingStats.create(testPlayer, 2023).apply {
                setStatsDirectly(gamesPlayed = 8, wins = 3, losses = 2)
            }
            val stats2024 = SeasonPitchingStats.create(testPlayer, 2024).apply {
                setStatsDirectly(gamesPlayed = 10, wins = 5, losses = 3)
            }

            every { playerStatsService.getAllSeasonPitchingStats(playerId) } returns
                listOf(stats2023, stats2024)

            // when & then
            mockMvc.perform(get("/api/v1/players/$playerId/stats/pitching/seasons"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].year").value(2023))
                .andExpect(jsonPath("$.data[0].wins").value(3))
                .andExpect(jsonPath("$.data[1].year").value(2024))
                .andExpect(jsonPath("$.data[1].wins").value(5))
        }

        @Test
        fun `should return empty list when no pitching stats exist`() {
            // given
            every { playerStatsService.getAllSeasonPitchingStats(playerId) } returns emptyList()

            // when & then
            mockMvc.perform(get("/api/v1/players/$playerId/stats/pitching/seasons"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    // Helper methods

    private fun SeasonBattingStats.setStatsDirectly(
        gamesPlayed: Int = 0,
        pa: Int = 0,
        atBats: Int = 0,
        hits: Int = 0,
        doubles: Int = 0,
        triples: Int = 0,
        homeRuns: Int = 0,
        runs: Int = 0,
        rbi: Int = 0,
        walks: Int = 0
    ) {
        val clazz = SeasonBattingStats::class.java
        setField(clazz, this, "gamesPlayed", gamesPlayed)
        setField(clazz, this, "plateAppearances", pa)
        setField(clazz, this, "atBats", atBats)
        setField(clazz, this, "hits", hits)
        setField(clazz, this, "doubles", doubles)
        setField(clazz, this, "triples", triples)
        setField(clazz, this, "homeRuns", homeRuns)
        setField(clazz, this, "runs", runs)
        setField(clazz, this, "runsBattedIn", rbi)
        setField(clazz, this, "walks", walks)
    }

    private fun SeasonPitchingStats.setStatsDirectly(
        gamesPlayed: Int = 0,
        gamesStarted: Int = 0,
        inningsPitchedOuts: Int = 0,
        wins: Int = 0,
        losses: Int = 0,
        saves: Int = 0,
        earnedRuns: Int = 0,
        strikeouts: Int = 0
    ) {
        val clazz = SeasonPitchingStats::class.java
        setField(clazz, this, "gamesPlayed", gamesPlayed)
        setField(clazz, this, "gamesStarted", gamesStarted)
        setField(clazz, this, "inningsPitchedOuts", inningsPitchedOuts)
        setField(clazz, this, "wins", wins)
        setField(clazz, this, "losses", losses)
        setField(clazz, this, "saves", saves)
        setField(clazz, this, "earnedRuns", earnedRuns)
        setField(clazz, this, "strikeouts", strikeouts)
    }

    private fun setField(clazz: Class<*>, obj: Any, fieldName: String, value: Any?) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
