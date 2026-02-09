package com.nextup.api.controller.attendance

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nextup.api.dto.attendance.CreatePollRequest
import com.nextup.api.dto.attendance.SubmitVoteRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.service.attendance.AttendanceService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@DisplayName("TeamAttendancePollController")
class TeamAttendancePollControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var attendanceService: AttendanceService
    private lateinit var controller: TeamAttendancePollController
    private lateinit var objectMapper: ObjectMapper

    private lateinit var team: Team
    private lateinit var player: Player
    private lateinit var poll: AttendancePoll

    @BeforeEach
    fun setUp() {
        attendanceService = mockk()
        controller = TeamAttendancePollController(attendanceService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        objectMapper =
            ObjectMapper()
                .registerKotlinModule()
                .registerModule(JavaTimeModule())

        val association = Association(name = "테스트 협회", id = 1L)
        val league =
            League(association = association, name = "테스트 리그", foundedYear = 2024, id = 1L)
        team = Team(league = league, name = "테스트 팀", city = "서울", foundedYear = 2024, id = 1L)
        player = Player(name = "홍길동", primaryPosition = Position.STARTING_PITCHER, id = 1L)
        poll =
            AttendancePoll.create(
                team = team,
                title = "이번 주 일요일 경기",
                eventDate = LocalDateTime.now().plusDays(7),
                deadline = LocalDateTime.now().plusDays(5),
            )
    }

    @Nested
    @DisplayName("POST /api/v1/teams/{teamId}/attendance-polls")
    inner class CreatePoll {
        @Test
        fun `출석 투표를 생성할 수 있다`() {
            // given
            val eventDate = LocalDateTime.now().plusDays(7)
            val deadline = LocalDateTime.now().plusDays(5)
            val request = CreatePollRequest("이번 주 일요일 경기", eventDate, deadline)
            every { attendanceService.createPoll(1L, any(), any(), any()) } returns poll

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/teams/1/attendance-polls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("이번 주 일요일 경기"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/attendance-polls")
    inner class ListPolls {
        @Test
        fun `팀의 투표 목록을 조회할 수 있다`() {
            // given
            every { attendanceService.listPolls(1L, null) } returns listOf(poll)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/attendance-polls"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("이번 주 일요일 경기"))
        }

        @Test
        fun `상태별로 투표 목록을 조회할 수 있다`() {
            // given
            every { attendanceService.listPolls(1L, PollStatus.OPEN) } returns listOf(poll)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/attendance-polls?status=OPEN"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/attendance-polls/{pollId}")
    inner class GetPoll {
        @Test
        fun `투표를 조회할 수 있다`() {
            // given
            every { attendanceService.getPoll(1L) } returns poll

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/attendance-polls/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("이번 주 일요일 경기"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/teams/{teamId}/attendance-polls/{pollId}/vote")
    inner class SubmitVote {
        @Test
        fun `투표에 응답할 수 있다`() {
            // given
            val request = SubmitVoteRequest(playerId = 1L, voteType = VoteType.ATTEND)
            val vote =
                AttendanceVote.create(poll = poll, player = player, voteType = VoteType.ATTEND)
            every { attendanceService.submitVote(1L, 1L, VoteType.ATTEND) } returns vote

            // when & then
            mockMvc
                .perform(
                    put("/api/v1/teams/1/attendance-polls/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voteType").value("ATTEND"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/teams/{teamId}/attendance-polls/{pollId}/close")
    inner class ClosePoll {
        @Test
        fun `투표를 마감할 수 있다`() {
            // given
            poll.close()
            every { attendanceService.closePoll(1L) } returns poll

            // when & then
            mockMvc
                .perform(put("/api/v1/teams/1/attendance-polls/1/close"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/attendance-polls/{pollId}/votes")
    inner class ListVotes {
        @Test
        fun `투표 응답 목록을 조회할 수 있다`() {
            // given
            val vote =
                AttendanceVote.create(poll = poll, player = player, voteType = VoteType.ATTEND)
            every { attendanceService.listVotes(1L) } returns listOf(vote)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/attendance-polls/1/votes"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].voteType").value("ATTEND"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/attendance-polls/{pollId}/statistics")
    inner class GetStatistics {
        @Test
        fun `투표 통계를 조회할 수 있다`() {
            // given
            val vote1 =
                AttendanceVote.create(poll = poll, player = player, voteType = VoteType.ATTEND)
            val vote2 =
                AttendanceVote.create(poll = poll, player = player, voteType = VoteType.ATTEND)
            val vote3 =
                AttendanceVote.create(poll = poll, player = player, voteType = VoteType.ABSENT)
            every { attendanceService.listVotes(1L) } returns listOf(vote1, vote2, vote3)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/attendance-polls/1/statistics"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalVotes").value(3))
                .andExpect(jsonPath("$.data.attendCount").value(2))
                .andExpect(jsonPath("$.data.absentCount").value(1))
                .andExpect(jsonPath("$.data.undecidedCount").value(0))
        }
    }
}
