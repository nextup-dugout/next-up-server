package com.nextup.api.controller.game

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.game.CreateBattingRecordRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.GamePlayerNotFoundByGameAndPlayerException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.service.game.BattingRecordService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("BattingRecordController 테스트")
class BattingRecordControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var battingRecordService: BattingRecordService
    private lateinit var objectMapper: ObjectMapper

    private val gameId = 1L
    private val playerId = 100L
    private val gamePlayerId = 10L

    @BeforeEach
    fun setUp() {
        battingRecordService = mockk()
        objectMapper = jacksonObjectMapper()

        val controller = BattingRecordController(battingRecordService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/batting-records")
    inner class GetBattingRecords {
        @Test
        fun `should return batting records for game`() {
            // given
            val gamePlayer =
                mockk<GamePlayer> {
                    every { id } returns gamePlayerId
                }
            val record =
                mockk<BattingRecord> {
                    every { id } returns 1L
                    every { this@mockk.gamePlayer } returns gamePlayer
                    every { createdAt } returns java.time.Instant.now()
                    every { updatedAt } returns java.time.Instant.now()
                    every { plateAppearances } returns 4
                    every { atBats } returns 3
                    every { hits } returns 1
                    every { doubles } returns 0
                    every { triples } returns 0
                    every { homeRuns } returns 0
                    every { runs } returns 1
                    every { runsBattedIn } returns 0
                    every { walks } returns 1
                    every { intentionalWalks } returns 0
                    every { hitByPitch } returns 0
                    every { strikeouts } returns 1
                    every { sacrificeBunts } returns 0
                    every { sacrificeFlies } returns 0
                    every { stolenBases } returns 0
                    every { caughtStealing } returns 0
                    every { groundedIntoDoublePlays } returns 0
                    every { singles } returns 1
                    every { totalBases } returns 1
                    every { extraBaseHits } returns 0
                    every { sacrifices } returns 0
                    every { totalWalks } returns 1
                    every { battingAverage } returns java.math.BigDecimal("0.333")
                    every { onBasePercentage } returns java.math.BigDecimal("0.500")
                    every { sluggingPercentage } returns java.math.BigDecimal("0.333")
                    every { ops } returns java.math.BigDecimal("0.833")
                    every { stolenBasePercentage } returns java.math.BigDecimal("0.000")
                }

            every { battingRecordService.getAllByGameId(gameId) } returns listOf(record)

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/batting-records"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].hits").value(1))
        }

        @Test
        fun `should return empty list when no records exist`() {
            // given
            every { battingRecordService.getAllByGameId(gameId) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/batting-records"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("POST /api/games/{gameId}/batting-records")
    inner class CreateBattingRecord {
        @Test
        fun `should create batting record successfully`() {
            // given
            val request = CreateBattingRecordRequest(playerId = playerId)
            val gamePlayer =
                mockk<GamePlayer> {
                    every { id } returns gamePlayerId
                }
            val record =
                mockk<BattingRecord> {
                    every { id } returns 1L
                    every { this@mockk.gamePlayer } returns gamePlayer
                    every { createdAt } returns java.time.Instant.now()
                    every { updatedAt } returns java.time.Instant.now()
                    every { plateAppearances } returns 0
                    every { atBats } returns 0
                    every { hits } returns 0
                    every { doubles } returns 0
                    every { triples } returns 0
                    every { homeRuns } returns 0
                    every { runs } returns 0
                    every { runsBattedIn } returns 0
                    every { walks } returns 0
                    every { intentionalWalks } returns 0
                    every { hitByPitch } returns 0
                    every { strikeouts } returns 0
                    every { sacrificeBunts } returns 0
                    every { sacrificeFlies } returns 0
                    every { stolenBases } returns 0
                    every { caughtStealing } returns 0
                    every { groundedIntoDoublePlays } returns 0
                    every { singles } returns 0
                    every { totalBases } returns 0
                    every { extraBaseHits } returns 0
                    every { sacrifices } returns 0
                    every { totalWalks } returns 0
                    every { battingAverage } returns java.math.BigDecimal("0.000")
                    every { onBasePercentage } returns java.math.BigDecimal("0.000")
                    every { sluggingPercentage } returns java.math.BigDecimal("0.000")
                    every { ops } returns java.math.BigDecimal("0.000")
                    every { stolenBasePercentage } returns java.math.BigDecimal("0.000")
                }

            every { battingRecordService.createRecordByGameAndPlayer(gameId, playerId) } returns record

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/games/$gameId/batting-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.gamePlayerId").value(gamePlayerId))
        }

        @Test
        fun `should return 404 when game player not found`() {
            // given
            val request = CreateBattingRecordRequest(playerId = playerId)
            every {
                battingRecordService.createRecordByGameAndPlayer(gameId, playerId)
            } throws GamePlayerNotFoundByGameAndPlayerException(gameId, playerId)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/games/$gameId/batting-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GAME_PLAYER_NOT_FOUND"))
        }

        @Test
        fun `should return 400 when record already exists`() {
            // given
            val request = CreateBattingRecordRequest(playerId = playerId)

            every {
                battingRecordService.createRecordByGameAndPlayer(gameId, playerId)
            } throws RecordAlreadyExistsException(gamePlayerId, "Batting")

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/games/$gameId/batting-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RECORD_ALREADY_EXISTS"))
        }
    }
}
