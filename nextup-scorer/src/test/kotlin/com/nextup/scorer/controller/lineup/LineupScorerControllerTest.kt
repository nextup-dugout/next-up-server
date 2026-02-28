package com.nextup.scorer.controller.lineup

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.common.exception.LineupNotExchangedException
import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.service.lineup.LineupService
import com.nextup.scorer.exception.GlobalExceptionHandler
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
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                    object : org.springframework.web.method.support.HandlerMethodArgumentResolver {
                        override fun supportsParameter(parameter: org.springframework.core.MethodParameter,): Boolean =
                            parameter.hasParameterAnnotation(
                                org.springframework.security.core.annotation.AuthenticationPrincipal::class.java,
                            )

                        override fun resolveArgument(
                            parameter: org.springframework.core.MethodParameter,
                            mavContainer: org.springframework.web.method.support.ModelAndViewContainer?,
                            webRequest: org.springframework.web.context.request.NativeWebRequest,
                            binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
                        ): Any = 2L
                    },
                ).build()

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
                every { exchangePendingAt } returns null
                every { exchangeRejectionReason } returns null
                every { exchangeRejectedBy } returns null
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
                .perform(get("/api/scorer/lineups/1"))
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
                    every { exchangePendingAt } returns null
                    every { exchangeRejectionReason } returns null
                    every { exchangeRejectedBy } returns null
                }

            every { lineupService.getSubmittedLineupsByGame(1L) } returns listOf(submittedSubmission)
            every { lineupService.getLineupEntries(any()) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(get("/api/scorer/lineups/submitted").param("gameId", "1"))
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
                .perform(get("/api/scorer/lineups/1/entries"))
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
                    every { exchangePendingAt } returns null
                    every { exchangeRejectionReason } returns null
                    every { exchangeRejectedBy } returns null
                }

            every { lineupService.confirmLineup(1L, 2L) } returns confirmedSubmission
            every { lineupService.getLineupEntries(1L) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(
                    post("/api/scorer/lineups/1/confirm"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
        }
    }

    @Nested
    inner class GetOpponentLineupTest {
        @Test
        fun `should return opponent lineup when exchange is complete`() {
            // given
            val exchangedSubmission =
                mockk<LineupSubmission>().apply {
                    every { id } returns 2L
                    every { game.id } returns 1L
                    every { team.id } returns 2L
                    every { team.name } returns "Lions"
                    every { submittedBy.id } returns 3L
                    every { submittedBy.nickname } returns "상대감독"
                    every { status } returns LineupSubmissionStatus.EXCHANGED
                    every { submittedAt } returns java.time.Instant.now()
                    every { confirmedAt } returns null
                    every { confirmedBy } returns null
                    every { rejectionReason } returns null
                    every { rejectedBy } returns null
                    every { exchangePendingAt } returns null
                    every { exchangeRejectionReason } returns null
                    every { exchangeRejectedBy } returns null
                }

            every { lineupService.getOpponentLineup(1L, 1L) } returns exchangedSubmission
            every { lineupService.getLineupEntries(2L) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(
                    get("/api/scorer/lineups/games/1/opponent-lineup")
                        .param("myTeamId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamName").value("Lions"))
                .andExpect(jsonPath("$.data.status").value("EXCHANGED"))
        }

        @Test
        fun `should return 403 when lineup exchange is not complete`() {
            // given
            every {
                lineupService.getOpponentLineup(1L, 1L)
            } throws LineupNotExchangedException(1L)

            // when & then
            mockMvc
                .perform(
                    get("/api/scorer/lineups/games/1/opponent-lineup")
                        .param("myTeamId", "1"),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LINEUP_NOT_EXCHANGED"))
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
                    every { exchangePendingAt } returns null
                    every { exchangeRejectionReason } returns null
                    every { exchangeRejectedBy } returns null
                }

            every { lineupService.rejectLineup(1L, 2L, "선수 등록번호 확인 필요") } returns rejectedSubmission
            every { lineupService.getLineupEntries(1L) } returns listOf(mockEntry)

            val request = mapOf("reason" to "선수 등록번호 확인 필요")

            // when & then
            mockMvc
                .perform(
                    post("/api/scorer/lineups/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("선수 등록번호 확인 필요"))
        }
    }
}
