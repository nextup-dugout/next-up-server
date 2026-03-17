package com.nextup.api.controller.player

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.service.game.GameLogEntry
import com.nextup.core.service.game.GameLogService
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

@DisplayName("GameLogController 테스트")
class GameLogControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var gameLogService: GameLogService

    private val playerId = 1L

    @BeforeEach
    fun setUp() {
        gameLogService = mockk()

        val controller = GameLogController(gameLogService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/game-log")
    inner class GetGameLog {
        @Test
        fun `should return game log for player`() {
            // given
            val opponentTeam =
                mockk<Team> {
                    every { name } returns "상대팀"
                }
            val opponentGameTeam =
                mockk<GameTeam> {
                    every { id } returns 2L
                    every { team } returns opponentTeam
                }
            val myTeam =
                mockk<Team> {
                    every { name } returns "우리팀"
                }
            val myGameTeam =
                mockk<GameTeam> {
                    every { id } returns 1L
                    every { team } returns myTeam
                    every { result } returns GameResult.WIN
                }
            val game =
                mockk<Game> {
                    every { id } returns 10L
                    every { scheduledAt } returns LocalDateTime.of(2025, 6, 15, 14, 0)
                    every { gameTeams } returns listOf(myGameTeam, opponentGameTeam)
                }
            every { myGameTeam.game } returns game
            val gamePlayer =
                mockk<GamePlayer> {
                    every { gameTeam } returns myGameTeam
                    every { position } returns Position.SHORTSTOP
                    every { battingOrder } returns 3
                }
            val battingRecord =
                mockk<BattingRecord> {
                    every { atBats } returns 4
                    every { hits } returns 2
                    every { runs } returns 1
                    every { runsBattedIn } returns 2
                    every { homeRuns } returns 1
                    every { walks } returns 0
                    every { strikeouts } returns 1
                    every { stolenBases } returns 0
                    every { battingAverage } returns BigDecimal("0.500")
                }
            val fieldingRecord =
                mockk<FieldingRecord> {
                    every { putOuts } returns 2
                    every { assists } returns 3
                    every { errors } returns 0
                    every { doublePlays } returns 1
                    every { fieldingPercentage } returns BigDecimal("1.000")
                }

            val entry =
                GameLogEntry(
                    gamePlayer = gamePlayer,
                    battingRecord = battingRecord,
                    pitchingRecord = null,
                    fieldingRecord = fieldingRecord,
                )
            every { gameLogService.getGameLog(playerId, 10) } returns listOf(entry)

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/game-log"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].gameId").value(10))
                .andExpect(jsonPath("$.data[0].opponentTeamName").value("상대팀"))
                .andExpect(jsonPath("$.data[0].result").value("승"))
                .andExpect(jsonPath("$.data[0].position").value("SHORTSTOP"))
                .andExpect(jsonPath("$.data[0].battingOrder").value(3))
                .andExpect(jsonPath("$.data[0].batting.atBats").value(4))
                .andExpect(jsonPath("$.data[0].batting.hits").value(2))
                .andExpect(jsonPath("$.data[0].batting.homeRuns").value(1))
                .andExpect(jsonPath("$.data[0].fielding.putOuts").value(2))
                .andExpect(jsonPath("$.data[0].fielding.errors").value(0))
                .andExpect(jsonPath("$.data[0].pitching").doesNotExist())
        }

        @Test
        fun `should return empty list when no games found`() {
            // given
            every { gameLogService.getGameLog(playerId, 10) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/game-log"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        fun `should accept custom limit parameter`() {
            // given
            every { gameLogService.getGameLog(playerId, 5) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/game-log?limit=5"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
        }
    }
}
