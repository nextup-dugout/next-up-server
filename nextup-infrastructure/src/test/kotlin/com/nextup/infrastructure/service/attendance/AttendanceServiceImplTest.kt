package com.nextup.infrastructure.service.attendance

import com.nextup.common.exception.AttendancePollClosedException
import com.nextup.common.exception.AttendancePollNotFoundException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.event.AttendanceVoteCreatedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.user.User
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
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
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
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
        teamMemberRepository = mockk()
        gameRepository = mockk()
        gameTeamRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        attendanceService =
            AttendanceServiceImpl(
                attendancePollRepository,
                attendanceVoteRepository,
                teamRepository,
                playerRepository,
                teamMemberRepository,
                gameRepository,
                gameTeamRepository,
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

    @Nested
    @DisplayName("getPoll")
    inner class GetPoll {
        @Test
        fun `투표를 ID로 조회할 수 있다`() {
            // given
            every { attendancePollRepository.findById(1L) } returns poll

            // when
            val result = attendanceService.getPoll(1L)

            // then
            assertThat(result.title).isEqualTo("테스트 투표")
        }

        @Test
        fun `투표가 존재하지 않으면 AttendancePollNotFoundException 발생`() {
            // given
            every { attendancePollRepository.findById(999L) } returns null

            // when & then
            assertThrows<AttendancePollNotFoundException> {
                attendanceService.getPoll(999L)
            }
        }
    }

    @Nested
    @DisplayName("listVotes")
    inner class ListVotes {
        @Test
        fun `투표의 응답 목록을 조회할 수 있다`() {
            // given
            val vote =
                AttendanceVote.create(poll = poll, player = player, voteType = VoteType.ATTEND)
            every { attendancePollRepository.existsById(1L) } returns true
            every { attendanceVoteRepository.findByPollId(1L) } returns listOf(vote)

            // when
            val result = attendanceService.listVotes(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].voteType).isEqualTo(VoteType.ATTEND)
        }

        @Test
        fun `투표가 존재하지 않으면 AttendancePollNotFoundException 발생`() {
            // given
            every { attendancePollRepository.existsById(999L) } returns false

            // when & then
            assertThrows<AttendancePollNotFoundException> {
                attendanceService.listVotes(999L)
            }
        }
    }

    @Nested
    @DisplayName("createPollsForGame")
    inner class CreatePollsForGame {
        @Test
        fun `경기에 대한 출석 투표를 양 팀 모두 생성할 수 있다`() {
            // given
            val gameId = 1L
            val game = createMockGame(gameId)
            val gameTeam1 = createMockGameTeam(10L)
            val gameTeam2 = createMockGameTeam(20L)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam1, gameTeam2)
            every { attendancePollRepository.existsByGameIdAndTeamId(gameId, 10L) } returns false
            every { attendancePollRepository.existsByGameIdAndTeamId(gameId, 20L) } returns false
            every { attendancePollRepository.save(any()) } returnsArgument 0

            // when
            val result = attendanceService.createPollsForGame(gameId)

            // then
            assertThat(result).hasSize(2)
            verify(exactly = 2) { attendancePollRepository.save(any()) }
        }

        @Test
        fun `이미 투표가 존재하는 팀은 스킵한다`() {
            // given
            val gameId = 1L
            val game = createMockGame(gameId)
            val gameTeam1 = createMockGameTeam(10L)
            val gameTeam2 = createMockGameTeam(20L)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam1, gameTeam2)
            every { attendancePollRepository.existsByGameIdAndTeamId(gameId, 10L) } returns true
            every { attendancePollRepository.existsByGameIdAndTeamId(gameId, 20L) } returns false
            every { attendancePollRepository.save(any()) } returnsArgument 0

            // when
            val result = attendanceService.createPollsForGame(gameId)

            // then
            assertThat(result).hasSize(1)
            verify(exactly = 1) { attendancePollRepository.save(any()) }
        }

        @Test
        fun `경기가 존재하지 않으면 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThrows<GameNotFoundException> {
                attendanceService.createPollsForGame(999L)
            }
        }

        @Test
        fun `팀 수가 2가 아니면 빈 리스트를 반환한다`() {
            // given
            val gameId = 1L
            val game = createMockGame(gameId)
            val gameTeam1 = createMockGameTeam(10L)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam1)

            // when
            val result = attendanceService.createPollsForGame(gameId)

            // then
            assertThat(result).isEmpty()
            verify(exactly = 0) { attendancePollRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("findGamePoll")
    inner class FindGamePoll {
        @Test
        fun `경기와 팀 ID로 투표를 조회할 수 있다`() {
            // given
            every { attendancePollRepository.findByGameIdAndTeamId(1L, 10L) } returns poll

            // when
            val result = attendanceService.findGamePoll(1L, 10L)

            // then
            assertThat(result).isNotNull
            assertThat(result!!.title).isEqualTo("테스트 투표")
        }

        @Test
        fun `투표가 없으면 null을 반환한다`() {
            // given
            every { attendancePollRepository.findByGameIdAndTeamId(1L, 10L) } returns null

            // when
            val result = attendanceService.findGamePoll(1L, 10L)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("closeExpiredPolls")
    inner class CloseExpiredPolls {
        @Test
        fun `만료된 투표를 마감할 수 있다`() {
            // given
            val poll1 = createMockOpenPoll(1L)
            val poll2 = createMockOpenPoll(2L)
            every { attendancePollRepository.findOpenPollsWithDeadlineBefore(any()) } returns
                listOf(poll1, poll2)
            every { attendancePollRepository.save(any()) } returnsArgument 0

            // when
            val result = attendanceService.closeExpiredPolls()

            // then
            assertThat(result).isEqualTo(2)
            verify(exactly = 2) { attendancePollRepository.save(any()) }
            verify(exactly = 1) { poll1.close() }
            verify(exactly = 1) { poll2.close() }
        }

        @Test
        fun `만료된 투표가 없으면 0을 반환한다`() {
            // given
            every { attendancePollRepository.findOpenPollsWithDeadlineBefore(any()) } returns
                emptyList()

            // when
            val result = attendanceService.closeExpiredPolls()

            // then
            assertThat(result).isEqualTo(0)
            verify(exactly = 0) { attendancePollRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("voteForGame")
    inner class VoteForGame {
        @Test
        fun `경기 출석 투표에 응답할 수 있다`() {
            // given
            val gameId = 1L
            val userId = 100L
            val playerId = 50L
            val teamId = 10L
            val pollId = 5L

            val game = createMockGame(gameId)
            val gameTeam = createMockGameTeam(teamId)
            val member = createMockTeamMember(teamId, userId, playerId)
            val gamePoll = createMockCanVotePoll(pollId)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(teamId) } returns listOf(member)
            every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns gamePoll
            every { attendancePollRepository.findById(pollId) } returns gamePoll
            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { attendanceVoteRepository.findByPollIdAndPlayerId(pollId, playerId) } returns null
            every { attendanceVoteRepository.save(any()) } returnsArgument 0

            // when
            val result =
                attendanceService.voteForGame(
                    gameId = gameId,
                    userId = userId,
                    voteType = VoteType.ATTEND,
                )

            // then
            assertThat(result.voteType).isEqualTo(VoteType.ATTEND)
        }

        @Test
        fun `경기가 없으면 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThrows<GameNotFoundException> {
                attendanceService.voteForGame(
                    gameId = 999L,
                    userId = 1L,
                    voteType = VoteType.ATTEND,
                )
            }
        }

        @Test
        fun `사용자가 경기 팀의 멤버가 아니면 ForbiddenException 발생`() {
            // given
            val gameId = 1L
            val game = createMockGame(gameId)
            val gameTeam = createMockGameTeam(10L)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(10L) } returns emptyList()

            // when & then
            assertThrows<ForbiddenException> {
                attendanceService.voteForGame(
                    gameId = gameId,
                    userId = 999L,
                    voteType = VoteType.ATTEND,
                )
            }
        }

        @Test
        fun `투표가 마감되면 AttendancePollClosedException 발생`() {
            // given
            val gameId = 1L
            val userId = 100L
            val teamId = 10L
            val pollId = 5L

            val game = createMockGame(gameId)
            val gameTeam = createMockGameTeam(teamId)
            val member = createMockTeamMember(teamId, userId, 50L)
            val closedPoll = createMockClosedPoll(pollId)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(teamId) } returns listOf(member)
            every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns closedPoll

            // when & then
            assertThrows<AttendancePollClosedException> {
                attendanceService.voteForGame(
                    gameId = gameId,
                    userId = userId,
                    voteType = VoteType.ATTEND,
                )
            }
        }
    }

    @Nested
    @DisplayName("getGameVotes")
    inner class GetGameVotes {
        @Test
        fun `경기의 투표 목록을 조회할 수 있다`() {
            // given
            val gameId = 1L
            val userId = 100L
            val teamId = 10L
            val pollId = 5L

            val game = createMockGame(gameId)
            val gameTeam = createMockGameTeam(teamId)
            val member = createMockTeamMember(teamId, userId, 50L)
            val gamePoll = createMockCanVotePoll(pollId)
            val vote = createMockVote(VoteType.ATTEND)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(teamId) } returns listOf(member)
            every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns gamePoll
            every { attendanceVoteRepository.findByPollId(pollId) } returns listOf(vote)

            // when
            val result = attendanceService.getGameVotes(gameId, userId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].voteType).isEqualTo(VoteType.ATTEND)
        }
    }

    @Nested
    @DisplayName("getGameVoteSummary")
    inner class GetGameVoteSummary {
        @Test
        fun `경기의 투표 요약을 조회할 수 있다`() {
            // given
            val gameId = 1L
            val userId = 100L
            val teamId = 10L
            val pollId = 5L

            val game = createMockGame(gameId)
            val gameTeam = createMockGameTeam(teamId)
            val member = createMockTeamMember(teamId, userId, 50L)
            val gamePoll = createMockCanVotePoll(pollId)

            val attendVote = createMockVote(VoteType.ATTEND)
            val absentVote = createMockVote(VoteType.ABSENT)
            val undecidedVote = createMockVote(VoteType.UNDECIDED)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(teamId) } returns listOf(member)
            every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns gamePoll
            every { attendanceVoteRepository.findByPollId(pollId) } returns
                listOf(attendVote, absentVote, undecidedVote)

            // when
            val result = attendanceService.getGameVoteSummary(gameId, userId)

            // then
            assertThat(result.pollId).isEqualTo(pollId)
            assertThat(result.gameId).isEqualTo(gameId)
            assertThat(result.totalVotes).isEqualTo(3)
            assertThat(result.attending).isEqualTo(1)
            assertThat(result.absent).isEqualTo(1)
            assertThat(result.undecided).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("getGameNonVoters")
    inner class GetGameNonVoters {
        @Test
        fun `경기의 미투표자 목록을 조회할 수 있다`() {
            // given
            val gameId = 1L
            val userId = 100L
            val teamId = 10L
            val pollId = 5L

            val game = createMockGame(gameId)
            val gameTeam = createMockGameTeam(teamId)
            val member = createMockTeamMember(teamId, userId, 50L)
            val gamePoll = createMockCanVotePoll(pollId)

            val attendVote = createMockVote(VoteType.ATTEND)
            val undecidedVote = createMockVote(VoteType.UNDECIDED)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(teamId) } returns listOf(member)
            every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns gamePoll
            every { attendanceVoteRepository.findByPollId(pollId) } returns
                listOf(attendVote, undecidedVote)

            // when
            val result = attendanceService.getGameNonVoters(gameId, userId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].voteType).isEqualTo(VoteType.UNDECIDED)
        }

        @Test
        fun `미투표자가 없으면 빈 리스트를 반환한다`() {
            // given
            val gameId = 1L
            val userId = 100L
            val teamId = 10L
            val pollId = 5L

            val game = createMockGame(gameId)
            val gameTeam = createMockGameTeam(teamId)
            val member = createMockTeamMember(teamId, userId, 50L)
            val gamePoll = createMockCanVotePoll(pollId)

            val attendVote = createMockVote(VoteType.ATTEND)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(teamId) } returns listOf(member)
            every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns gamePoll
            every { attendanceVoteRepository.findByPollId(pollId) } returns listOf(attendVote)

            // when
            val result = attendanceService.getGameNonVoters(gameId, userId)

            // then
            assertThat(result).isEmpty()
        }
    }

    // === 헬퍼 메서드 ===

    private fun createMockGame(
        id: Long,
        scheduledAt: LocalDateTime = LocalDateTime.now().plusDays(7),
    ): Game {
        val game: Game = mockk(relaxed = true)
        every { game.id } returns id
        every { game.scheduledAt } returns scheduledAt
        every { game.status } returns GameStatus.SCHEDULED
        return game
    }

    private fun createMockGameTeam(teamId: Long): GameTeam {
        val mockTeam: Team = mockk(relaxed = true)
        every { mockTeam.id } returns teamId

        val gameTeam: GameTeam = mockk(relaxed = true)
        every { gameTeam.team } returns mockTeam
        return gameTeam
    }

    private fun createMockTeamMember(
        teamId: Long,
        userId: Long,
        playerId: Long,
    ): TeamMember {
        val mockUser: User = mockk(relaxed = true)
        every { mockUser.id } returns userId

        val mockTeam: Team = mockk(relaxed = true)
        every { mockTeam.id } returns teamId

        val mockPlayer: Player = mockk(relaxed = true)
        every { mockPlayer.id } returns playerId

        val member: TeamMember = mockk(relaxed = true)
        every { member.user } returns mockUser
        every { member.team } returns mockTeam
        every { member.player } returns mockPlayer
        return member
    }

    private fun createMockCanVotePoll(pollId: Long): AttendancePoll {
        val mockPoll: AttendancePoll = mockk(relaxed = true)
        every { mockPoll.id } returns pollId
        every { mockPoll.canVote() } returns true
        every { mockPoll.isOpen() } returns true
        every { mockPoll.status } returns PollStatus.OPEN
        return mockPoll
    }

    private fun createMockClosedPoll(pollId: Long): AttendancePoll {
        val mockPoll: AttendancePoll = mockk(relaxed = true)
        every { mockPoll.id } returns pollId
        every { mockPoll.canVote() } returns false
        every { mockPoll.isOpen() } returns false
        every { mockPoll.status } returns PollStatus.CLOSED
        return mockPoll
    }

    private fun createMockOpenPoll(pollId: Long): AttendancePoll {
        val mockPoll: AttendancePoll = mockk(relaxed = true)
        every { mockPoll.id } returns pollId
        every { mockPoll.canVote() } returns true
        every { mockPoll.isOpen() } returns true
        every { mockPoll.status } returns PollStatus.OPEN
        return mockPoll
    }

    private fun createMockVote(voteType: VoteType): AttendanceVote {
        val mockPlayer: Player = mockk(relaxed = true)
        every { mockPlayer.id } returns 1L
        every { mockPlayer.name } returns "테스트 선수"

        val vote: AttendanceVote = mockk(relaxed = true)
        every { vote.player } returns mockPlayer
        every { vote.voteType } returns voteType
        every { vote.isAttending() } returns (voteType == VoteType.ATTEND)
        every { vote.isAbsent() } returns (voteType == VoteType.ABSENT)
        return vote
    }
}
