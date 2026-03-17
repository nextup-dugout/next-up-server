package com.nextup.backoffice.controller.correction

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.backoffice.dto.correction.CorrectFieldingRecordRequest
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.service.game.correction.FieldingCorrectionRequest
import com.nextup.core.service.game.correction.RecordCorrectionDto
import com.nextup.core.service.game.correction.RecordCorrectionService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("RecordCorrectionAdminController")
class RecordCorrectionAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var recordCorrectionService: RecordCorrectionService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        recordCorrectionService = mockk()
        val controller = RecordCorrectionAdminController(recordCorrectionService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper()
    }

    @Nested
    @DisplayName("PUT /api/backoffice/games/{gameId}/fielding-records/{recordId}")
    inner class CorrectFieldingRecord {
        @Test
        fun `수비 기록 정정 요청이 성공하면 200과 정정된 기록을 반환한다`() {
            // given
            val gameId = 1L
            val recordId = 10L
            val fieldingRecord = createFieldingRecord(recordId)

            every {
                recordCorrectionService.correctFieldingRecord(
                    gameId = gameId,
                    recordId = recordId,
                    request =
                        FieldingCorrectionRequest(
                            adminUserId = 100L,
                            fieldName = "putOuts",
                            newValue = "5",
                            reason = "기록원 오류 정정",
                        ),
                )
            } returns fieldingRecord

            val request =
                CorrectFieldingRecordRequest(
                    adminUserId = 100L,
                    fieldName = "putOuts",
                    newValue = "5",
                    reason = "기록원 오류 정정",
                )

            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/games/$gameId/fielding-records/$recordId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recordId").value(recordId))
                .andExpect(jsonPath("$.data.putOuts").value(fieldingRecord.putOuts))
                .andExpect(jsonPath("$.data.assists").value(fieldingRecord.assists))
                .andExpect(jsonPath("$.data.errors").value(fieldingRecord.errors))
                .andExpect(jsonPath("$.data.doublePlays").value(fieldingRecord.doublePlays))
                .andExpect(jsonPath("$.data.passedBalls").value(fieldingRecord.passedBalls))
                .andExpect(jsonPath("$.data.totalChances").value(fieldingRecord.totalChances))

            verify(exactly = 1) {
                recordCorrectionService.correctFieldingRecord(
                    gameId = gameId,
                    recordId = recordId,
                    request = any(),
                )
            }
        }

        @Test
        fun `assists 필드를 정정할 수 있다`() {
            // given
            val gameId = 2L
            val recordId = 20L
            val fieldingRecord = createFieldingRecord(recordId, assists = 3)

            every {
                recordCorrectionService.correctFieldingRecord(
                    gameId = gameId,
                    recordId = recordId,
                    request = any(),
                )
            } returns fieldingRecord

            val request =
                CorrectFieldingRecordRequest(
                    adminUserId = 100L,
                    fieldName = "assists",
                    newValue = "3",
                    reason = "정정 사유",
                )

            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/games/$gameId/fielding-records/$recordId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assists").value(3))
        }

        @Test
        fun `adminUserId가 없으면 400을 반환한다`() {
            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/games/1/fielding-records/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "fieldName": "putOuts",
                                "newValue": "5",
                                "reason": "정정 사유"
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isBadRequest)
        }

        @Test
        fun `fieldName이 없으면 400을 반환한다`() {
            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/games/1/fielding-records/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "adminUserId": 100,
                                "newValue": "5",
                                "reason": "정정 사유"
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isBadRequest)
        }

        @Test
        fun `reason이 없으면 400을 반환한다`() {
            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/games/1/fielding-records/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "adminUserId": 100,
                                "fieldName": "putOuts",
                                "newValue": "5"
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isBadRequest)
        }

        @Test
        fun `fieldName이 빈 문자열이면 400을 반환한다`() {
            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/games/1/fielding-records/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "adminUserId": 100,
                                "fieldName": "",
                                "newValue": "5",
                                "reason": "정정 사유"
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/games/{gameId}/corrections")
    inner class GetCorrectionHistory {
        @Test
        fun `정정 이력 조회가 성공하면 200과 이력 목록을 반환한다`() {
            // given
            val gameId = 1L
            val history =
                listOf(
                    RecordCorrectionDto(
                        id = 1L,
                        gameId = gameId,
                        adminUserId = 100L,
                        correctionType = CorrectionType.FIELDING,
                        targetRecordId = 10L,
                        fieldName = "putOuts",
                        oldValue = "2",
                        newValue = "5",
                        reason = "기록원 오류 정정",
                    ),
                )

            every { recordCorrectionService.getCorrectionHistory(gameId) } returns history

            // when & then
            mockMvc
                .perform(get("/api/backoffice/games/$gameId/corrections"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].fieldName").value("putOuts"))
                .andExpect(jsonPath("$.data[0].oldValue").value("2"))
                .andExpect(jsonPath("$.data[0].newValue").value("5"))
                .andExpect(jsonPath("$.data[0].reason").value("기록원 오류 정정"))

            verify(exactly = 1) { recordCorrectionService.getCorrectionHistory(gameId) }
        }

        @Test
        fun `정정 이력이 없으면 빈 배열을 반환한다`() {
            // given
            val gameId = 99L
            every { recordCorrectionService.getCorrectionHistory(gameId) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/backoffice/games/$gameId/corrections"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    // ========== Helper Methods ==========

    private fun createFieldingRecord(
        id: Long,
        putOuts: Int = 2,
        assists: Int = 1,
        errors: Int = 0,
        doublePlays: Int = 0,
        passedBalls: Int = 0,
    ): FieldingRecord {
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns 1L
        val record = FieldingRecord.create(gamePlayer)
        setField(record, "id", id)
        repeat(putOuts) { record.recordPutOut() }
        repeat(assists) { record.recordAssist() }
        repeat(errors) { record.recordError() }
        repeat(doublePlays) { record.recordDoublePlay() }
        repeat(passedBalls) { record.recordPassedBall() }
        return record
    }

    private fun setField(
        obj: Any,
        fieldName: String,
        value: Any,
    ) {
        val clazz = obj.javaClass
        val field =
            generateSequence<Class<*>>(clazz) { it.superclass }
                .flatMap { it.declaredFields.asSequence() }
                .first { it.name == fieldName }
        field.isAccessible = true
        field.set(obj, value)
    }
}
