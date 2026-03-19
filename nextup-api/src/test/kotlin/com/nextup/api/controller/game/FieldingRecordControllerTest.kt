package com.nextup.api.controller.game

import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.service.game.FieldingRecordService
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

@DisplayName("FieldingRecordController 테스트")
class FieldingRecordControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var fieldingRecordService: FieldingRecordService

    private val gameId = 1L
    private val gamePlayerId = 10L

    @BeforeEach
    fun setUp() {
        fieldingRecordService = mockk()

        val controller = FieldingRecordController(fieldingRecordService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/fielding-records")
    inner class GetFieldingRecords {
        @Test
        fun `should return fielding records for game`() {
            // given
            val gamePlayer =
                mockk<GamePlayer> {
                    every { id } returns gamePlayerId
                }
            val record =
                mockk<FieldingRecord> {
                    every { id } returns 1L
                    every { this@mockk.gamePlayer } returns gamePlayer
                    every { createdAt } returns java.time.Instant.now()
                    every { updatedAt } returns java.time.Instant.now()
                    every { putOuts } returns 3
                    every { assists } returns 2
                    every { errors } returns 1
                    every { doublePlays } returns 0
                    every { passedBalls } returns 0
                    every { totalChances } returns 6
                    every { fieldingPercentage } returns BigDecimal("0.833")
                }

            every { fieldingRecordService.getAllByGameId(gameId) } returns listOf(record)

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/fielding-records"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].gamePlayerId").value(gamePlayerId))
                .andExpect(jsonPath("$.data[0].putOuts").value(3))
                .andExpect(jsonPath("$.data[0].assists").value(2))
                .andExpect(jsonPath("$.data[0].errors").value(1))
                .andExpect(jsonPath("$.data[0].totalChances").value(6))
                .andExpect(jsonPath("$.data[0].fieldingPercentage").value("0.833"))
        }

        @Test
        fun `should return empty list when no records exist`() {
            // given
            every { fieldingRecordService.getAllByGameId(gameId) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/fielding-records"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        fun `should handle null fielding percentage`() {
            // given
            val gamePlayer =
                mockk<GamePlayer> {
                    every { id } returns gamePlayerId
                }
            val record =
                mockk<FieldingRecord> {
                    every { id } returns 1L
                    every { this@mockk.gamePlayer } returns gamePlayer
                    every { createdAt } returns java.time.Instant.now()
                    every { updatedAt } returns java.time.Instant.now()
                    every { putOuts } returns 0
                    every { assists } returns 0
                    every { errors } returns 0
                    every { doublePlays } returns 0
                    every { passedBalls } returns 0
                    every { totalChances } returns 0
                    every { fieldingPercentage } returns null
                }

            every { fieldingRecordService.getAllByGameId(gameId) } returns listOf(record)

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/fielding-records"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].totalChances").value(0))
        }
    }
}
