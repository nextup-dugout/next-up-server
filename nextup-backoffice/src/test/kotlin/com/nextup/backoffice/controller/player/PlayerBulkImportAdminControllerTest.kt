package com.nextup.backoffice.controller.player

import com.nextup.backoffice.exception.GlobalExceptionHandler
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.service.player.PlayerBulkImportService
import com.nextup.core.service.player.PlayerImportError
import com.nextup.core.service.player.PlayerImportResult
import com.nextup.core.service.player.PlayerImportRow
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("PlayerBulkImportAdminController")
class PlayerBulkImportAdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var playerBulkImportService: PlayerBulkImportService
    private lateinit var controller: PlayerBulkImportAdminController

    @BeforeEach
    fun setUp() {
        playerBulkImportService = mockk()
        controller = PlayerBulkImportAdminController(playerBulkImportService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("POST /api/backoffice/import/players")
    inner class ImportPlayers {

        @Test
        fun `CSV 파일로 선수를 일괄 등록할 수 있다`() {
            // given
            val csvContent =
                """
                name,primary_position,birth_date,height,weight,throwing_hand,batting_hand
                홍길동,SHORTSTOP,1990-01-15,180,75,RIGHT,RIGHT
                김철수,STARTING_PITCHER,,,,LEFT,
                """.trimIndent()

            val file =
                MockMultipartFile(
                    "file",
                    "players.csv",
                    "text/csv",
                    csvContent.toByteArray(Charsets.UTF_8),
                )

            val importedPlayer = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP)
            val rowsSlot = slot<List<PlayerImportRow>>()
            every { playerBulkImportService.importPlayers(capture(rowsSlot)) } returns
                PlayerImportResult(
                    successCount = 2,
                    errorCount = 0,
                    importedPlayers = listOf(importedPlayer),
                    errors = emptyList(),
                )

            // when & then
            mockMvc.perform(
                multipart("/api/backoffice/import/players").file(file),
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.errorCount").value(0))
                .andExpect(jsonPath("$.data.errors").isArray)

            val capturedRows = rowsSlot.captured
            assertThat(capturedRows).hasSize(2)
            assertThat(capturedRows[0].name).isEqualTo("홍길동")
            assertThat(capturedRows[0].primaryPosition).isEqualTo("SHORTSTOP")
            assertThat(capturedRows[0].birthDate).isEqualTo("1990-01-15")
            assertThat(capturedRows[1].name).isEqualTo("김철수")
            assertThat(capturedRows[1].birthDate).isNull()
        }

        @Test
        fun `일부 오류가 있어도 성공한 행을 임포트하고 결과를 반환한다`() {
            // given
            val csvContent =
                """
                name,primary_position,birth_date,height,weight,throwing_hand,batting_hand
                홍길동,INVALID_POSITION,,,,,
                """.trimIndent()

            val file =
                MockMultipartFile(
                    "file",
                    "players.csv",
                    "text/csv",
                    csvContent.toByteArray(Charsets.UTF_8),
                )

            every { playerBulkImportService.importPlayers(any()) } returns
                PlayerImportResult(
                    successCount = 0,
                    errorCount = 1,
                    importedPlayers = emptyList(),
                    errors = listOf(PlayerImportError(rowNumber = 2, reason = "유효하지 않은 포지션")),
                )

            // when & then
            mockMvc.perform(
                multipart("/api/backoffice/import/players").file(file),
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.errorCount").value(1))
                .andExpect(jsonPath("$.data.errors[0].rowNumber").value(2))
                .andExpect(jsonPath("$.data.errors[0].reason").value("유효하지 않은 포지션"))
        }

        @Test
        fun `빈 파일을 업로드하면 400 오류가 반환된다`() {
            // given
            val file =
                MockMultipartFile(
                    "file",
                    "empty.csv",
                    "text/csv",
                    ByteArray(0),
                )

            // when & then
            mockMvc.perform(
                multipart("/api/backoffice/import/players").file(file),
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun `헤더만 있고 데이터가 없는 CSV 파일은 400 오류가 반환된다`() {
            // given
            val csvContent = "name,primary_position,birth_date,height,weight,throwing_hand,batting_hand"
            val file =
                MockMultipartFile(
                    "file",
                    "header_only.csv",
                    "text/csv",
                    csvContent.toByteArray(Charsets.UTF_8),
                )

            // when & then
            mockMvc.perform(
                multipart("/api/backoffice/import/players").file(file),
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
