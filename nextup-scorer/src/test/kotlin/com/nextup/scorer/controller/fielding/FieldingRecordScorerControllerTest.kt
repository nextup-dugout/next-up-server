package com.nextup.scorer.controller.fielding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.common.exception.GamePlayerNotFoundByGameAndPlayerException
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.service.game.FieldingRecordService
import com.nextup.scorer.dto.fielding.FieldingEventRequest
import com.nextup.scorer.dto.fielding.FieldingEventType
import com.nextup.scorer.exception.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

@DisplayName("FieldingRecordScorerController 테스트")
class FieldingRecordScorerControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var fieldingRecordService: FieldingRecordService
    private lateinit var objectMapper: ObjectMapper

    private val gameId = 1L
    private val playerId = 10L
    private val gamePlayerId = 100L

    @BeforeEach
    fun setUp() {
        fieldingRecordService = mockk()
        objectMapper = jacksonObjectMapper()

        val controller =
            FieldingRecordScorerController(
                fieldingRecordService = fieldingRecordService,
            )
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/fielding/players/{playerId}")
    inner class CreateFieldingRecord {
        @Test
        fun `수비 기록을 성공적으로 생성한다`() {
            // given
            val mockGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { mockGamePlayer.id } returns gamePlayerId

            val mockRecord = mockk<FieldingRecord>(relaxed = true)
            every { mockRecord.id } returns 1L
            every { mockRecord.gamePlayer } returns mockGamePlayer
            every { mockRecord.putOuts } returns 0
            every { mockRecord.assists } returns 0
            every { mockRecord.errors } returns 0
            every { mockRecord.doublePlays } returns 0
            every { mockRecord.passedBalls } returns 0
            every { mockRecord.totalChances } returns 0
            every { mockRecord.fieldingPercentage } returns null

            every { fieldingRecordService.createRecordByGameAndPlayer(gameId, playerId) } returns mockRecord

            // when & then
            mockMvc
                .perform(post("/api/v1/scorer/games/$gameId/fielding/players/$playerId"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.gamePlayerId").value(gamePlayerId))
                .andExpect(jsonPath("$.data.putOuts").value(0))
                .andExpect(jsonPath("$.data.assists").value(0))
                .andExpect(jsonPath("$.data.errors").value(0))

            verify(exactly = 1) { fieldingRecordService.createRecordByGameAndPlayer(gameId, playerId) }
        }

        @Test
        fun `GamePlayer를 찾을 수 없으면 404를 반환한다`() {
            // given
            every {
                fieldingRecordService.createRecordByGameAndPlayer(gameId, playerId)
            } throws GamePlayerNotFoundByGameAndPlayerException(gameId, playerId)

            // when & then
            mockMvc
                .perform(post("/api/v1/scorer/games/$gameId/fielding/players/$playerId"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/fielding/events")
    inner class RecordFieldingEvent {
        @Test
        fun `자살(PUT_OUT) 이벤트를 성공적으로 기록한다`() {
            // given
            val request = FieldingEventRequest(gamePlayerId = gamePlayerId, eventType = FieldingEventType.PUT_OUT)
            val mockGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { mockGamePlayer.id } returns gamePlayerId

            val mockRecord = mockk<FieldingRecord>(relaxed = true)
            every { mockRecord.id } returns 1L
            every { mockRecord.gamePlayer } returns mockGamePlayer
            every { mockRecord.putOuts } returns 1
            every { mockRecord.assists } returns 0
            every { mockRecord.errors } returns 0
            every { mockRecord.doublePlays } returns 0
            every { mockRecord.passedBalls } returns 0
            every { mockRecord.totalChances } returns 1
            every { mockRecord.fieldingPercentage } returns java.math.BigDecimal("1.000")

            every { fieldingRecordService.recordPutOut(gamePlayerId) } returns Unit
            every { fieldingRecordService.getByGamePlayerId(gamePlayerId) } returns mockRecord

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/scorer/games/$gameId/fielding/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.putOuts").value(1))
                .andExpect(jsonPath("$.data.fieldingPercentage").value("1.000"))

            verify(exactly = 1) { fieldingRecordService.recordPutOut(gamePlayerId) }
        }

        @Test
        fun `보살(ASSIST) 이벤트를 성공적으로 기록한다`() {
            // given
            val request = FieldingEventRequest(gamePlayerId = gamePlayerId, eventType = FieldingEventType.ASSIST)
            val mockGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { mockGamePlayer.id } returns gamePlayerId

            val mockRecord = mockk<FieldingRecord>(relaxed = true)
            every { mockRecord.id } returns 1L
            every { mockRecord.gamePlayer } returns mockGamePlayer
            every { mockRecord.putOuts } returns 0
            every { mockRecord.assists } returns 1
            every { mockRecord.errors } returns 0
            every { mockRecord.doublePlays } returns 0
            every { mockRecord.passedBalls } returns 0
            every { mockRecord.totalChances } returns 1
            every { mockRecord.fieldingPercentage } returns java.math.BigDecimal("1.000")

            every { fieldingRecordService.recordAssist(gamePlayerId) } returns Unit
            every { fieldingRecordService.getByGamePlayerId(gamePlayerId) } returns mockRecord

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/scorer/games/$gameId/fielding/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assists").value(1))

            verify(exactly = 1) { fieldingRecordService.recordAssist(gamePlayerId) }
        }

        @Test
        fun `실책(ERROR) 이벤트를 성공적으로 기록한다`() {
            // given
            val request = FieldingEventRequest(gamePlayerId = gamePlayerId, eventType = FieldingEventType.ERROR)
            val mockGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { mockGamePlayer.id } returns gamePlayerId

            val mockRecord = mockk<FieldingRecord>(relaxed = true)
            every { mockRecord.id } returns 1L
            every { mockRecord.gamePlayer } returns mockGamePlayer
            every { mockRecord.putOuts } returns 0
            every { mockRecord.assists } returns 0
            every { mockRecord.errors } returns 1
            every { mockRecord.doublePlays } returns 0
            every { mockRecord.passedBalls } returns 0
            every { mockRecord.totalChances } returns 1
            every { mockRecord.fieldingPercentage } returns java.math.BigDecimal("0.000")

            every { fieldingRecordService.recordError(gamePlayerId) } returns Unit
            every { fieldingRecordService.getByGamePlayerId(gamePlayerId) } returns mockRecord

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/scorer/games/$gameId/fielding/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.errors").value(1))

            verify(exactly = 1) { fieldingRecordService.recordError(gamePlayerId) }
        }

        @Test
        fun `병살(DOUBLE_PLAY) 이벤트를 성공적으로 기록한다`() {
            // given
            val request =
                FieldingEventRequest(gamePlayerId = gamePlayerId, eventType = FieldingEventType.DOUBLE_PLAY)
            val mockGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { mockGamePlayer.id } returns gamePlayerId

            val mockRecord = mockk<FieldingRecord>(relaxed = true)
            every { mockRecord.id } returns 1L
            every { mockRecord.gamePlayer } returns mockGamePlayer
            every { mockRecord.putOuts } returns 0
            every { mockRecord.assists } returns 0
            every { mockRecord.errors } returns 0
            every { mockRecord.doublePlays } returns 1
            every { mockRecord.passedBalls } returns 0
            every { mockRecord.totalChances } returns 0
            every { mockRecord.fieldingPercentage } returns null

            every { fieldingRecordService.recordDoublePlay(gamePlayerId) } returns Unit
            every { fieldingRecordService.getByGamePlayerId(gamePlayerId) } returns mockRecord

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/scorer/games/$gameId/fielding/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.doublePlays").value(1))

            verify(exactly = 1) { fieldingRecordService.recordDoublePlay(gamePlayerId) }
        }

        @Test
        fun `포일(PASSED_BALL) 이벤트를 성공적으로 기록한다`() {
            // given
            val request =
                FieldingEventRequest(gamePlayerId = gamePlayerId, eventType = FieldingEventType.PASSED_BALL)
            val mockGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { mockGamePlayer.id } returns gamePlayerId

            val mockRecord = mockk<FieldingRecord>(relaxed = true)
            every { mockRecord.id } returns 1L
            every { mockRecord.gamePlayer } returns mockGamePlayer
            every { mockRecord.putOuts } returns 0
            every { mockRecord.assists } returns 0
            every { mockRecord.errors } returns 0
            every { mockRecord.doublePlays } returns 0
            every { mockRecord.passedBalls } returns 1
            every { mockRecord.totalChances } returns 0
            every { mockRecord.fieldingPercentage } returns null

            every { fieldingRecordService.recordPassedBall(gamePlayerId) } returns Unit
            every { fieldingRecordService.getByGamePlayerId(gamePlayerId) } returns mockRecord

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/scorer/games/$gameId/fielding/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.passedBalls").value(1))

            verify(exactly = 1) { fieldingRecordService.recordPassedBall(gamePlayerId) }
        }
    }

    @Nested
    @DisplayName("GET /api/scorer/games/{gameId}/fielding")
    inner class GetFieldingRecordsByGame {
        @Test
        fun `경기의 수비 기록을 성공적으로 조회한다`() {
            // given
            val mockGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { mockGamePlayer.id } returns gamePlayerId

            val mockRecord = mockk<FieldingRecord>(relaxed = true)
            every { mockRecord.id } returns 1L
            every { mockRecord.gamePlayer } returns mockGamePlayer
            every { mockRecord.putOuts } returns 5
            every { mockRecord.assists } returns 3
            every { mockRecord.errors } returns 1
            every { mockRecord.doublePlays } returns 1
            every { mockRecord.passedBalls } returns 0
            every { mockRecord.totalChances } returns 9
            every { mockRecord.fieldingPercentage } returns java.math.BigDecimal("0.889")

            every { fieldingRecordService.getAllByGameId(gameId) } returns listOf(mockRecord)

            // when & then
            mockMvc
                .perform(get("/api/v1/scorer/games/$gameId/fielding"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].putOuts").value(5))
                .andExpect(jsonPath("$.data[0].assists").value(3))
                .andExpect(jsonPath("$.data[0].errors").value(1))
                .andExpect(jsonPath("$.data[0].totalChances").value(9))
        }

        @Test
        fun `경기의 수비 기록이 없으면 빈 리스트를 반환한다`() {
            // given
            every { fieldingRecordService.getAllByGameId(gameId) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/scorer/games/$gameId/fielding"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }
}
