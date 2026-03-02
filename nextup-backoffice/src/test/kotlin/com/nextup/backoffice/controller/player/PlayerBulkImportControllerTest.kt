package com.nextup.backoffice.controller.player

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.backoffice.dto.player.PlayerBulkImportRequest
import com.nextup.backoffice.dto.player.PlayerBulkImportResponse
import com.nextup.backoffice.dto.player.PlayerImportItem
import com.nextup.backoffice.dto.player.PlayerImportResult
import com.nextup.backoffice.service.PlayerBulkImportService
import com.nextup.core.domain.player.Position
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

@DisplayName("PlayerBulkImportController")
class PlayerBulkImportControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var playerBulkImportService: PlayerBulkImportService
    private lateinit var controller: PlayerBulkImportController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        playerBulkImportService = mockk()
        controller = PlayerBulkImportController(playerBulkImportService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Nested
    @DisplayName("POST /api/backoffice/players/bulk-import")
    inner class BulkImport {
        @Test
        @DisplayName("선수 목록을 일괄 등록할 수 있다")
        fun `should bulk import players successfully`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                        birthDate = LocalDate.of(1990, 5, 15),
                        height = 180,
                        weight = 80,
                    ),
                    PlayerImportItem(
                        name = "김철수",
                        primaryPosition = Position.CATCHER,
                    ),
                )
            val request = PlayerBulkImportRequest(players = items)

            val response =
                PlayerBulkImportResponse(
                    totalRequested = 2,
                    successCount = 2,
                    failureCount = 0,
                    importedPlayers =
                        listOf(
                            PlayerImportResult(
                                id = 1L,
                                name = "홍길동",
                                primaryPosition = "유격수",
                                birthDate = LocalDate.of(1990, 5, 15),
                            ),
                            PlayerImportResult(
                                id = 2L,
                                name = "김철수",
                                primaryPosition = "포수",
                                birthDate = null,
                            ),
                        ),
                    failures = emptyList(),
                )

            every { playerBulkImportService.importPlayers(items) } returns response

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/players/bulk-import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRequested").value(2))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failureCount").value(0))
                .andExpect(jsonPath("$.data.importedPlayers").isArray)
                .andExpect(jsonPath("$.data.importedPlayers[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.importedPlayers[1].name").value("김철수"))

            verify(exactly = 1) { playerBulkImportService.importPlayers(items) }
        }

        @Test
        @DisplayName("일부 실패 시에도 성공한 항목이 포함된 결과를 반환한다")
        fun `should return partial success response`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                    ),
                    PlayerImportItem(
                        name = "김철수",
                        primaryPosition = Position.CATCHER,
                    ),
                )
            val request = PlayerBulkImportRequest(players = items)

            val response =
                PlayerBulkImportResponse(
                    totalRequested = 2,
                    successCount = 1,
                    failureCount = 1,
                    importedPlayers =
                        listOf(
                            PlayerImportResult(
                                id = 1L,
                                name = "홍길동",
                                primaryPosition = "유격수",
                                birthDate = null,
                            ),
                        ),
                    failures =
                        listOf(
                            com.nextup.backoffice.dto.player.PlayerImportFailure(
                                rowIndex = 1,
                                name = "김철수",
                                reason = "중복 선수",
                            ),
                        ),
                )

            every { playerBulkImportService.importPlayers(items) } returns response

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/players/bulk-import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRequested").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(1))
                .andExpect(jsonPath("$.data.failures[0].rowIndex").value(1))
                .andExpect(jsonPath("$.data.failures[0].reason").value("중복 선수"))
        }
    }
}
