package com.nextup.infrastructure.service.attendance

import com.nextup.common.exception.AttendancePollClosedException
import com.nextup.common.exception.AttendancePollNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.event.AttendanceVoteCreatedEvent
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@DisplayName("AttendanceServiceImpl")
class AttendanceServiceImplTest {
    private lateinit var attendancePollRepository: AttendancePollRepositoryPort
    private lateinit var attendanceVoteRepository: AttendanceVoteRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var attendanceService: AttendanceServiceImpl

    private lateinit var team: Team
    private lateinit var player: Player
    private lateinit var poll: AttendancePoll

    @BeforeEach
    fun setUp() {
        attendancePollRepository = mockk()
        attendanceVoteRepository = mockk()
        teamRepository = mockk()
        playerRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        attendanceService =
            AttendanceServiceImpl(
                attendancePollRepository,
                attendanceVoteRepository,
                teamRepository,
                playerRepository,
                eventPublisher,
            )

        val association = Association(name = "테스트협회", region = "서울")
        val league = League(association = association, name = "테스트 리그", foundedYear = 2024)
        team = Team(league = league, name = "테스트 팀", city = "서울", foundedYear = 2024)
        player = Player(name = "홍길동", primaryPosition = Position.STARTING_PITCHER)
        poll =
            AttendancePoll.create(
                team = team,
                title = "테스트 투표",
                eventDate = LocalDateTime.now().plusDays(7),
                deadline = LocalDateTime.now().plusDays(5),
            )
    }

    @Nested
    @DisplayName("createPoll")
    inner class CreatePoll {
        @Test
        fun `출석 투표를 생성할 수 있다`() {
            // given
            val eventDate = LocalDateTime.now().plusDays(7)
            val deadline = LocalDateTime.now().plusDays(5)
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { attendancePollRepository.save(any()) } returnsArgument 0

            // when
            val result =
                attendanceService.createPoll(
                    teamId = 1L,
                    title = "이번 주 일요일 경기",
                    eventDate = eventDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    deadline = deadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                )

            // then
            assertThat(result.title).isEqualTo("이번 주 일요일 경기")
            assertThat(result.status).isEqualTo(PollStatus.OPEN)
            verify { attendancePollRepository.save(any()) }
        }

        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException 발생`() {
            // given
            val eventDate = LocalDateTime.now().plusDays(7)
            val deadline = LocalDateTime.now().plusDays(5)
            every { teamRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThrows<TeamNotFoundException> {
                attendanceService.createPoll(
                    teamId = 999L,
                    title = "테스트",
                    eventDate = eventDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    deadline = deadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                )
            }
        }

        @Test
        fun `출석 투표 생성 시 AttendanceVoteCreatedEvent 이벤트가 발행된다`() {
            // given
            val eventDate = LocalDateTime.now().plusDays(7)
            val deadline = LocalDateTime.now().plusDays(5)
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { attendancePollRepository.save(any()) } returnsArgument 0

            val eventSlot = slot<AttendanceVoteCreatedEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            // when
            attendanceService.createPoll(
                teamId = 1L,
                title = "이번 주 경기",
                eventDate = eventDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                deadline = deadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            )

            // then
            verify(exactly = 1) { eventPublisher.publishEvent(any<AttendanceVoteCreatedEvent>()) }
            assertThat(eventSlot.captured.teamId).isEqualTo(1L)
            assertThat(eventSlot.captured.eventDate).isEqualTo(eventDate)
        }
    }

    @Nested
    @DisplayName("submitVote")
    inner class SubmitVote {
        @Test
        fun `출석 투표에 응답할 수 있다`() {
            // given
            every { attendancePollRepository.findById(1L) } returns poll
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { attendanceVoteRepository.findByPollIdAndPlayerId(1L, 1L) } returns null
            every { attendanceVoteRepository.save(any()) } returnsArgument 0

            // when
            val result =
                attendanceService.submitVote(
                    pollId = 1L,
                    playerId = 1L,
                    voteType = VoteType.ATTEND,
                )

            // then
            assertThat(result.voteType).isEqualTo(VoteType.ATTEND)
            verify { attendanceVoteRepository.save(any()) }
        }

        @Test
        fun `이미 투표한 경우 투표를 변경할 수 있다`() {
            // given
            val existingVote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.UNDECIDED,
                )
            every { attendancePollRepository.findById(1L) } returns poll
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { attendanceVoteRepository.findByPollIdAndPlayerId(1L, 1L) } returns existingVote
            every { attendanceVoteRepository.save(any()) } returnsArgument 0

            // when
            val result =
                attendanceService.submitVote(
                    pollId = 1L,
                    playerId = 1L,
                    voteType = VoteType.ATTEND,
                )

            // then
            assertThat(result.voteType).isEqualTo(VoteType.ATTEND)
        }

        @Test
        fun `투표가 마감되면 AttendancePollClosedException 발생`() {
            // given
            poll.close()
            every { attendancePollRepository.findById(1L) } returns poll

            // when & then
            assertThrows<AttendancePollClosedException> {
                attendanceService.submitVote(
                    pollId = 1L,
                    playerId = 1L,
                    voteType = VoteType.ATTEND,
                )
            }
        }

        @Test
        fun `투표가 존재하지 않으면 AttendancePollNotFoundException 발생`() {
            // given
            every { attendancePollRepository.findById(999L) } returns null

            // when & then
            assertThrows<AttendancePollNotFoundException> {
                attendanceService.submitVote(
                    pollId = 999L,
                    playerId = 1L,
                    voteType = VoteType.ATTEND,
                )
            }
        }

        @Test
        fun `선수가 존재하지 않으면 PlayerNotFoundException 발생`() {
            // given
            every { attendancePollRepository.findById(1L) } returns poll
            every { playerRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThrows<PlayerNotFoundException> {
                attendanceService.submitVote(
                    pollId = 1L,
                    playerId = 999L,
                    voteType = VoteType.ATTEND,
                )
            }
        }
    }

    @Nested
    @DisplayName("listPolls")
    inner class ListPolls {
        @Test
        fun `팀의 모든 투표를 조회할 수 있다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            every { attendancePollRepository.findByTeamId(1L, null) } returns listOf(poll)

            // when
            val result = attendanceService.listPolls(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("테스트 투표")
        }

        @Test
        fun `상태별로 투표를 조회할 수 있다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns listOf(poll)

            // when
            val result = attendanceService.listPolls(1L, PollStatus.OPEN)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(PollStatus.OPEN)
        }

        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException 발생`() {
            // given
            every { teamRepository.existsById(999L) } returns false

            // when & then
            assertThrows<TeamNotFoundException> {
                attendanceService.listPolls(999L)
            }
        }
    }

    @Nested
    @DisplayName("closePoll")
    inner class ClosePoll {
        @Test
        fun `투표를 마감할 수 있다`() {
            // given
            every { attendancePollRepository.findById(1L) } returns poll
            every { attendancePollRepository.save(any()) } returnsArgument 0

            // when
            val result = attendanceService.closePoll(1L)

            // then
            assertThat(result.status).isEqualTo(PollStatus.CLOSED)
            verify { attendancePollRepository.save(any()) }
        }

        @Test
        fun `투표가 존재하지 않으면 AttendancePollNotFoundException 발생`() {
            // given
            every { attendancePollRepository.findById(999L) } returns null

            // when & then
            assertThrows<AttendancePollNotFoundException> {
                attendanceService.closePoll(999L)
            }
        }
    }
}
