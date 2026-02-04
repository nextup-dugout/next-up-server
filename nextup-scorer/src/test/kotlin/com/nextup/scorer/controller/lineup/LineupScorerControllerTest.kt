package com.nextup.scorer.controller.lineup

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.service.lineup.LineupService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class LineupScorerControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var lineupService: LineupService
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockSubmission: LineupSubmission
    private lateinit var mockEntry: LineupEntry

    @BeforeEach
    fun setUp() {
        lineupService = mockk()
        objectMapper = ObjectMapper()

        val controller = LineupScorerController(lineupService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

        // Mock submission
        mockSubmission =
            mockk<LineupSubmission>().apply {
                every { id } returns 1L
                every { game.id } returns 1L
                every { team.id } returns 1L
                every { team.name } returns "Tigers"
                every { submittedBy.id } returns 1L
                every { submittedBy.nickname } returns "감독"
                every { status } returns LineupSubmissionStatus.DRAFT
                every { submittedAt } returns null
                every { confirmedAt } returns null
                every { confirmedBy } returns null
                every { rejectionReason } returns null
                every { rejectedBy } returns null
            }

        // Mock entry
        mockEntry =
            mockk<LineupEntry>().apply {
                every { id } returns 1L
                every { player.id } returns 1L
                every { player.name } returns "홍길동"
                every { position } returns Position.SHORTSTOP
                every { battingOrder } returns 1
                every { backNumber } returns 7
                every { isStarter } returns true
            }
    }

    @Nested
    inner class GetLineupTest {
        @Test
        fun `should get lineup by id`() {
            // given
            every { lineupService.getLineupSubmission(1L) } returns mockSubmission
            every { lineupService.getLineupEntries(1L) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(get("/api/v1/scorer/lineups/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.entries[0].playerName").value("홍길동"))
        }
    }

    @Nested
    inner class GetSubmittedLineupsTest {
        @Test
        fun `should get submitted lineups`() {
            // given
            val submittedSubmission =
                mockk<LineupSubmission>().apply {
                    every { id } returns 1L
                    every { game.id } returns 1L
                    every { team.id } returns 1L
                    every { team.name } returns "Tigers"
                    every { submittedBy.id } returns 1L
                    every { submittedBy.nickname } returns "감독"
                    every { status } returns LineupSubmissionStatus.SUBMITTED
                    every { submittedAt } returns java.time.Instant.now()
                    every { confirmedAt } returns null
                    every { confirmedBy } returns null
                    every { rejectionReason } returns null
                    every { rejectedBy } returns null
                }

            every { lineupService.getSubmittedLineupsByGame(1L) } returns listOf(submittedSubmission)
            every { lineupService.getLineupEntries(any()) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(get("/api/v1/scorer/lineups/submitted").param("gameId", "1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("SUBMITTED"))
        }
    }

    @Nested
    inner class GetLineupEntriesTest {
        @Test
        fun `should get lineup entries`() {
            // given
            every { lineupService.getLineupEntries(1L) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(get("/api/v1/scorer/lineups/1/entries"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].playerName").value("홍길동"))
        }
    }

    @Nested
    inner class ConfirmLineupTest {
        @Test
        fun `should confirm lineup`() {
            // given
            val confirmedSubmission =
                mockk<LineupSubmission>().apply {
                    every { id } returns 1L
                    every { game.id } returns 1L
                    every { team.id } returns 1L
                    every { team.name } returns "Tigers"
                    every { submittedBy.id } returns 1L
                    every { submittedBy.nickname } returns "감독"
                    every { status } returns LineupSubmissionStatus.CONFIRMED
                    every { submittedAt } returns java.time.Instant.now()
                    every { confirmedAt } returns java.time.Instant.now()
                    every { confirmedBy?.id } returns 2L
                    every { confirmedBy?.nickname } returns "기록원"
                    every { rejectionReason } returns null
                    every { rejectedBy } returns null
                }

            every { lineupService.confirmLineup(1L, 2L) } returns confirmedSubmission
            every { lineupService.getLineupEntries(1L) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/scorer/lineups/1/confirm")
                        .header("X-User-Id", 2L),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
        }
    }

    @Nested
    inner class RejectLineupTest {
        @Test
        fun `should reject lineup`() {
            // given
            val rejectedSubmission =
                mockk<LineupSubmission>().apply {
                    every { id } returns 1L
                    every { game.id } returns 1L
                    every { team.id } returns 1L
                    every { team.name } returns "Tigers"
                    every { submittedBy.id } returns 1L
                    every { submittedBy.nickname } returns "감독"
                    every { status } returns LineupSubmissionStatus.REJECTED
                    every { submittedAt } returns java.time.Instant.now()
                    every { confirmedAt } returns null
                    every { confirmedBy } returns null
                    every { rejectionReason } returns "선수 등록번호 확인 필요"
                    every { rejectedBy?.id } returns 2L
                    every { rejectedBy?.nickname } returns "기록원"
                }

            every { lineupService.rejectLineup(1L, 2L, "선수 등록번호 확인 필요") } returns rejectedSubmission
            every { lineupService.getLineupEntries(1L) } returns listOf(mockEntry)

            val request = mapOf("reason" to "선수 등록번호 확인 필요")

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/scorer/lineups/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", 2L)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("선수 등록번호 확인 필요"))
        }
    }
}
