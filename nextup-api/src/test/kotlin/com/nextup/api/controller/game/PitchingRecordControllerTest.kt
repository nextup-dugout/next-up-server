package com.nextup.api.controller.game

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.game.CreatePitchingRecordRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.infrastructure.repository.game.GamePlayerRepository
import com.nextup.core.service.game.PitchingRecordService
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

@DisplayName("PitchingRecordController 테스트")
class PitchingRecordControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var pitchingRecordService: PitchingRecordService
    private lateinit var gamePlayerRepository: GamePlayerRepository
    private lateinit var objectMapper: ObjectMapper

    private val gameId = 1L
    private val playerId = 100L
    private val gamePlayerId = 10L

    @BeforeEach
    fun setUp() {
        pitchingRecordService = mockk()
        gamePlayerRepository = mockk()
        objectMapper = jacksonObjectMapper()

        val controller = PitchingRecordController(pitchingRecordService, gamePlayerRepository)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/pitching-records")
    inner class GetPitchingRecords {

        @Test
        fun `should return pitching records for game`() {
            // given
            val gamePlayer = mockk<GamePlayer> {
                every { id } returns gamePlayerId
            }
            val record = mockk<PitchingRecord> {
                every { id } returns 1L
                every { this@mockk.gamePlayer } returns gamePlayer
                every { createdAt } returns java.time.Instant.now()
                every { updatedAt } returns java.time.Instant.now()
                every { inningsPitchedOuts } returns 15
                every { earnedRuns } returns 2
                every { runsAllowed } returns 3
                every { hitsAllowed } returns 5
                every { walksAllowed } returns 2
                every { strikeouts } returns 6
                every { homeRunsAllowed } returns 1
                every { hitBatsmen } returns 0
                every { wildPitches } returns 1
                every { balks } returns 0
                every { battersFaced } returns 22
                every { decision } returns PitchingDecision.WIN
                every { isStartingPitcher } returns true
                every { pitchesThrown } returns 85
                every { strikesThrown } returns 55
                every { completeInnings } returns 5
                every { remainingOuts } returns 0
                every { inningsPitched } returns java.math.BigDecimal("5.00")
                every { inningsPitchedDisplay } returns "5.0"
                every { earnedRunAverage } returns java.math.BigDecimal("3.60")
                every { whip } returns java.math.BigDecimal("1.40")
                every { strikeoutsPer9 } returns java.math.BigDecimal("10.80")
                every { walksPer9 } returns java.math.BigDecimal("3.60")
                every { strikeoutToWalkRatio } returns java.math.BigDecimal("3.00")
                every { strikePercentage } returns java.math.BigDecimal("0.647")
                every { unearnedRuns } returns 1
                every { isQualifiedForWin } returns true
            }

            every { pitchingRecordService.getAllByGameId(gameId) } returns listOf(record)

            // when & then
            mockMvc.perform(get("/api/games/$gameId/pitching-records"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].isStartingPitcher").value(true))
                .andExpect(jsonPath("$.data[0].decision").value("WIN"))
        }

        @Test
        fun `should return empty list when no records exist`() {
            // given
            every { pitchingRecordService.getAllByGameId(gameId) } returns emptyList()

            // when & then
            mockMvc.perform(get("/api/games/$gameId/pitching-records"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("POST /api/games/{gameId}/pitching-records")
    inner class CreatePitchingRecord {

        @Test
        fun `should create starting pitcher record successfully`() {
            // given
            val request = CreatePitchingRecordRequest(playerId = playerId, isStartingPitcher = true)
            val gamePlayer = mockk<GamePlayer> {
                every { id } returns gamePlayerId
            }
            val record = createMockPitchingRecord(gamePlayer, isStartingPitcher = true)

            every { gamePlayerRepository.findByGameIdAndPlayerId(gameId, playerId) } returns gamePlayer
            every { pitchingRecordService.createRecord(gamePlayerId, true) } returns record

            // when & then
            mockMvc.perform(
                post("/api/games/$gameId/pitching-records")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isStartingPitcher").value(true))
        }

        @Test
        fun `should create relief pitcher record successfully`() {
            // given
            val request = CreatePitchingRecordRequest(playerId = playerId, isStartingPitcher = false)
            val gamePlayer = mockk<GamePlayer> {
                every { id } returns gamePlayerId
            }
            val record = createMockPitchingRecord(gamePlayer, isStartingPitcher = false)

            every { gamePlayerRepository.findByGameIdAndPlayerId(gameId, playerId) } returns gamePlayer
            every { pitchingRecordService.createRecord(gamePlayerId, false) } returns record

            // when & then
            mockMvc.perform(
                post("/api/games/$gameId/pitching-records")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isStartingPitcher").value(false))
        }

        @Test
        fun `should return 404 when game player not found`() {
            // given
            val request = CreatePitchingRecordRequest(playerId = playerId)
            every { gamePlayerRepository.findByGameIdAndPlayerId(gameId, playerId) } returns null

            // when & then
            mockMvc.perform(
                post("/api/games/$gameId/pitching-records")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GAME_PLAYER_NOT_FOUND"))
        }

        @Test
        fun `should return 400 when record already exists`() {
            // given
            val request = CreatePitchingRecordRequest(playerId = playerId)
            val gamePlayer = mockk<GamePlayer> {
                every { id } returns gamePlayerId
            }

            every { gamePlayerRepository.findByGameIdAndPlayerId(gameId, playerId) } returns gamePlayer
            every { pitchingRecordService.createRecord(gamePlayerId, false) } throws
                RecordAlreadyExistsException(gamePlayerId, "Pitching")

            // when & then
            mockMvc.perform(
                post("/api/games/$gameId/pitching-records")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RECORD_ALREADY_EXISTS"))
        }
    }

    private fun createMockPitchingRecord(gamePlayer: GamePlayer, isStartingPitcher: Boolean): PitchingRecord {
        return mockk {
            every { id } returns 1L
            every { this@mockk.gamePlayer } returns gamePlayer
            every { createdAt } returns java.time.Instant.now()
            every { updatedAt } returns java.time.Instant.now()
            every { inningsPitchedOuts } returns 0
            every { earnedRuns } returns 0
            every { runsAllowed } returns 0
            every { hitsAllowed } returns 0
            every { walksAllowed } returns 0
            every { strikeouts } returns 0
            every { homeRunsAllowed } returns 0
            every { hitBatsmen } returns 0
            every { wildPitches } returns 0
            every { balks } returns 0
            every { battersFaced } returns 0
            every { decision } returns PitchingDecision.NONE
            every { this@mockk.isStartingPitcher } returns isStartingPitcher
            every { pitchesThrown } returns null
            every { strikesThrown } returns null
            every { completeInnings } returns 0
            every { remainingOuts } returns 0
            every { inningsPitched } returns java.math.BigDecimal("0.00")
            every { inningsPitchedDisplay } returns "0.0"
            every { earnedRunAverage } returns java.math.BigDecimal("0.00")
            every { whip } returns java.math.BigDecimal("0.00")
            every { strikeoutsPer9 } returns java.math.BigDecimal("0.00")
            every { walksPer9 } returns java.math.BigDecimal("0.00")
            every { strikeoutToWalkRatio } returns java.math.BigDecimal("0.00")
            every { strikePercentage } returns null
            every { unearnedRuns } returns 0
            every { isQualifiedForWin } returns false
        }
    }
}
