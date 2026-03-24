package com.nextup.api.controller.game

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nextup.api.dto.attendance.AttendanceVoteRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.AttendancePollClosedException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.player.Player
import com.nextup.core.service.attendance.AttendanceService
import com.nextup.core.service.attendance.GameVoteSummary
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("AttendanceController (game) 테스트")
class AttendanceControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var attendanceService: AttendanceService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        attendanceService = mockk()
        objectMapper = ObjectMapper().registerKotlinModule()

        val controller = AttendanceController(attendanceService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
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
    }

    @Nested
    @DisplayName("POST /api/v1/games/{gameId}/attendance")
    inner class Vote {
        @Test
        fun `출석 투표에 응답할 수 있다`() {
            // given
            val gameId = 1L
            val request = AttendanceVoteRequest(voteType = VoteType.ATTEND)
            val vote = createMockVote(VoteType.ATTEND, 1L, "홍길동")

            every {
                attendanceService.voteForGame(
                    gameId = gameId,
                    userId = 1L,
                    voteType = VoteType.ATTEND,
                    absenceReason = null,
                    reasonDetail = null,
                )
            } returns vote

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/games/{gameId}/attendance", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voteType").value("ATTEND"))
                .andExpect(jsonPath("$.data.gameId").value(1))
        }

        @Test
        fun `경기가 존재하지 않으면 404를 반환한다`() {
            // given
            val gameId = 999L
            val request = AttendanceVoteRequest(voteType = VoteType.ATTEND)

            every {
                attendanceService.voteForGame(
                    gameId = gameId,
                    userId = 1L,
                    voteType = VoteType.ATTEND,
                    absenceReason = null,
                    reasonDetail = null,
                )
            } throws GameNotFoundException(gameId)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/games/{gameId}/attendance", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun `팀 멤버가 아니면 403을 반환한다`() {
            // given
            val gameId = 1L
            val request = AttendanceVoteRequest(voteType = VoteType.ATTEND)

            every {
                attendanceService.voteForGame(
                    gameId = gameId,
                    userId = 1L,
                    voteType = VoteType.ATTEND,
                    absenceReason = null,
                    reasonDetail = null,
                )
            } throws ForbiddenException("GAME_MEMBER_001", "You are not a member of either team in this game")

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/games/{gameId}/attendance", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun `투표가 마감되면 400을 반환한다`() {
            // given
            val gameId = 1L
            val request = AttendanceVoteRequest(voteType = VoteType.ATTEND)

            every {
                attendanceService.voteForGame(
                    gameId = gameId,
                    userId = 1L,
                    voteType = VoteType.ATTEND,
                    absenceReason = null,
                    reasonDetail = null,
                )
            } throws AttendancePollClosedException(1L)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/games/{gameId}/attendance", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/games/{gameId}/attendance")
    inner class ChangeVote {
        @Test
        fun `출석 투표를 변경할 수 있다`() {
            // given
            val gameId = 1L
            val request = AttendanceVoteRequest(voteType = VoteType.ABSENT)
            val vote = createMockVote(VoteType.ABSENT, 1L, "홍길동")

            every {
                attendanceService.voteForGame(
                    gameId = gameId,
                    userId = 1L,
                    voteType = VoteType.ABSENT,
                    absenceReason = null,
                    reasonDetail = null,
                )
            } returns vote

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/games/{gameId}/attendance", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voteType").value("ABSENT"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/attendance")
    inner class GetVotes {
        @Test
        fun `경기의 투표 현황을 조회할 수 있다`() {
            // given
            val gameId = 1L
            val vote1 = createMockVote(VoteType.ATTEND, 1L, "홍길동")
            val vote2 = createMockVote(VoteType.ABSENT, 2L, "김철수")
            val summary =
                GameVoteSummary(
                    pollId = 10L,
                    gameId = gameId,
                    totalVotes = 2,
                    attending = 1,
                    absent = 1,
                    undecided = 0,
                )

            every { attendanceService.getGameVotes(gameId, 1L) } returns listOf(vote1, vote2)
            every { attendanceService.getGameVoteSummary(gameId, 1L) } returns summary

            // when & then
            mockMvc
                .perform(get("/api/v1/games/{gameId}/attendance", gameId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameId").value(1))
                .andExpect(jsonPath("$.data.pollId").value(10))
                .andExpect(jsonPath("$.data.votes.length()").value(2))
                .andExpect(jsonPath("$.data.summary.attending").value(1))
                .andExpect(jsonPath("$.data.summary.absent").value(1))
                .andExpect(jsonPath("$.data.summary.undecided").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/attendance/summary")
    inner class GetSummary {
        @Test
        fun `투표 요약을 조회할 수 있다`() {
            // given
            val gameId = 1L
            val summary =
                GameVoteSummary(
                    pollId = 10L,
                    gameId = gameId,
                    totalVotes = 5,
                    attending = 3,
                    absent = 1,
                    undecided = 1,
                )

            every { attendanceService.getGameVoteSummary(gameId, 1L) } returns summary

            // when & then
            mockMvc
                .perform(get("/api/v1/games/{gameId}/attendance/summary", gameId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pollId").value(10))
                .andExpect(jsonPath("$.data.totalVotes").value(5))
                .andExpect(jsonPath("$.data.attending").value(3))
                .andExpect(jsonPath("$.data.absent").value(1))
                .andExpect(jsonPath("$.data.undecided").value(1))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/attendance/non-voters")
    inner class GetNonVoters {
        @Test
        fun `미투표자 목록을 조회할 수 있다`() {
            // given
            val gameId = 1L
            val vote1 = createMockVote(VoteType.UNDECIDED, 1L, "홍길동")
            val vote2 = createMockVote(VoteType.UNDECIDED, 2L, "김철수")

            every { attendanceService.getGameNonVoters(gameId, 1L) } returns listOf(vote1, vote2)

            // when & then
            mockMvc
                .perform(get("/api/v1/games/{gameId}/attendance/non-voters", gameId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].playerName").value("홍길동"))
                .andExpect(jsonPath("$.data[1].playerName").value("김철수"))
        }

        @Test
        fun `미투표자가 없으면 빈 리스트를 반환한다`() {
            // given
            val gameId = 1L

            every { attendanceService.getGameNonVoters(gameId, 1L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/games/{gameId}/attendance/non-voters", gameId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    private fun createMockVote(
        voteType: VoteType,
        playerId: Long,
        playerName: String,
    ): AttendanceVote {
        val player: Player = mockk(relaxed = true)
        every { player.id } returns playerId
        every { player.name } returns playerName

        val vote: AttendanceVote = mockk(relaxed = true)
        every { vote.id } returns playerId
        every { vote.player } returns player
        every { vote.voteType } returns voteType
        every { vote.absenceReason } returns null
        every { vote.reasonDetail } returns null
        return vote
    }
}
