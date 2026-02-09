package com.nextup.api.controller.lineup

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class LineupControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var lineupService: LineupService
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockSubmission: LineupSubmission
    private lateinit var mockEntry: LineupEntry

    @BeforeEach
    fun setUp() {
        lineupService = mockk()
        objectMapper = ObjectMapper()

        val controller = LineupController(lineupService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                    object : org.springframework.web.method.support.HandlerMethodArgumentResolver {
                        override fun supportsParameter(parameter: org.springframework.core.MethodParameter): Boolean =
                            parameter.hasParameterAnnotation(
                                org.springframework.security.core.annotation.AuthenticationPrincipal::class.java,
                            )

                        override fun resolveArgument(
                            parameter: org.springframework.core.MethodParameter,
                            mavContainer: org.springframework.web.method.support.ModelAndViewContainer?,
                            webRequest: org.springframework.web.context.request.NativeWebRequest,
                            binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
                        ): Any = 1L
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
    inner class CreateLineupTest {
        @Test
        fun `should create lineup successfully`() {
            // given
            val request =
                mapOf(
                    "gameId" to 1L,
                    "teamId" to 1L,
                )

            every {
                lineupService.createLineupSubmission(
                    gameId = 1L,
                    teamId = 1L,
                    submittedByUserId = 1L,
                )
            } returns mockSubmission
            every { lineupService.getLineupEntries(1L) } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/lineups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.teamName").value("Tigers"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
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
                .perform(get("/api/v1/lineups/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.entries[0].playerName").value("홍길동"))
        }
    }

    @Nested
    inner class GetLineupsByGameTest {
        @Test
        fun `should get lineups by game`() {
            // given
            every { lineupService.getLineupSubmissionsByGame(1L) } returns listOf(mockSubmission)
            every { lineupService.getLineupEntries(any()) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(get("/api/v1/lineups").param("gameId", "1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].starterCount").value(1))
        }
    }

    @Nested
    inner class AddLineupEntryTest {
        @Test
        fun `should add lineup entry`() {
            // given
            val request =
                mapOf(
                    "playerId" to 1L,
                    "position" to "SHORTSTOP",
                    "battingOrder" to 1,
                    "backNumber" to 7,
                    "isStarter" to true,
                )

            every {
                lineupService.addLineupEntry(
                    submissionId = 1L,
                    playerId = 1L,
                    position = Position.SHORTSTOP,
                    battingOrder = 1,
                    backNumber = 7,
                    isStarter = true,
                )
            } returns mockEntry

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/lineups/1/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerName").value("홍길동"))
                .andExpect(jsonPath("$.data.position").value("SHORTSTOP"))
        }
    }

    @Nested
    inner class SetLineupEntriesTest {
        @Test
        fun `should set lineup entries`() {
            // given
            val entries =
                (1..9).map { order ->
                    mapOf(
                        "playerId" to order.toLong(),
                        "position" to Position.SHORTSTOP.name,
                        "battingOrder" to order,
                        "backNumber" to order,
                        "isStarter" to true,
                    )
                }
            val request = mapOf("entries" to entries)

            val mockEntries =
                (1..9).map { order ->
                    mockk<LineupEntry>().apply {
                        every { id } returns order.toLong()
                        every { player.id } returns order.toLong()
                        every { player.name } returns "선수$order"
                        every { position } returns Position.SHORTSTOP
                        every { battingOrder } returns order
                        every { backNumber } returns order
                        every { isStarter } returns true
                    }
                }

            every { lineupService.setLineupEntries(1L, any()) } returns mockEntries

            // when & then
            mockMvc
                .perform(
                    put("/api/v1/lineups/1/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(9))
                .andExpect(jsonPath("$.data[0].playerName").value("선수1"))
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
                .perform(get("/api/v1/lineups/1/entries"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].playerName").value("홍길동"))
        }
    }

    @Nested
    inner class SubmitLineupTest {
        @Test
        fun `should submit lineup`() {
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

            every { lineupService.submitLineup(1L) } returns submittedSubmission
            every { lineupService.getLineupEntries(1L) } returns listOf(mockEntry)

            // when & then
            mockMvc
                .perform(post("/api/v1/lineups/1/submit"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
        }
    }
}
