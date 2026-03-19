package com.nextup.infrastructure.listener

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.competition.BracketEntry
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionStatus
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.TeamDisbandedEvent
import com.nextup.core.domain.event.TeamMemberKickedEvent
import com.nextup.core.domain.event.TeamMemberLeftEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamJoinRequest
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.user.User
import com.nextup.core.port.attendance.ActivityScoreRepositoryPort
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.repository.BracketEntryRepositoryPort
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.LineupEntryRepositoryPort
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import com.nextup.core.port.repository.TeamJoinRequestRepositoryPort
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("TeamMemberEventListener")
class TeamMemberEventListenerTest {
    private val lineupSubmissionRepository = mockk<LineupSubmissionRepositoryPort>()
    private val lineupEntryRepository = mockk<LineupEntryRepositoryPort>()
    private val competitionPlayerRepository = mockk<CompetitionPlayerRepositoryPort>()
    private val competitionRepository = mockk<CompetitionRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val bracketEntryRepository = mockk<BracketEntryRepositoryPort>()
    private val stadiumBookingRepository = mockk<StadiumBookingRepositoryPort>()
    private val attendancePollRepository = mockk<AttendancePollRepositoryPort>()
    private val electionRepository = mockk<ElectionRepositoryPort>()
    private val teamJoinRequestRepository = mockk<TeamJoinRequestRepositoryPort>()
    private val activityScoreRepository = mockk<ActivityScoreRepositoryPort>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val listener =
        TeamMemberEventListener(
            lineupSubmissionRepository = lineupSubmissionRepository,
            lineupEntryRepository = lineupEntryRepository,
            competitionPlayerRepository = competitionPlayerRepository,
            competitionRepository = competitionRepository,
            gameRepository = gameRepository,
            bracketEntryRepository = bracketEntryRepository,
            stadiumBookingRepository = stadiumBookingRepository,
            attendancePollRepository = attendancePollRepository,
            electionRepository = electionRepository,
            teamJoinRequestRepository = teamJoinRequestRepository,
            activityScoreRepository = activityScoreRepository,
            eventPublisher = eventPublisher,
        )

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var team: Team
    private lateinit var user: User
    private lateinit var player: Player
    private lateinit var member: TeamMember

    @BeforeEach
    fun setUp() {
        association = Association(name = "서울야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "타이거즈", city = "서울", foundedYear = 2015)
        setFieldId(team, Team::class.java, 1L)

        user = User.createLocalUser("user@example.com", "password", "테스트유저")
        setFieldId(user, User::class.java, 10L)

        player = Player(name = "테스트유저", primaryPosition = Position.SHORTSTOP)
        setFieldId(player, Player::class.java, 20L)
        user.player = player

        member = TeamMember.create(team, user, player, 7, TeamMemberRole.MEMBER)
        setFieldId(member, TeamMember::class.java, 100L)
    }

    // -------------------------------------------------------------------------
    // TeamMemberLeftEvent 처리 테스트
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleTeamMemberLeft")
    inner class HandleTeamMemberLeft {
        @Test
        @DisplayName("DRAFT 라인업에서 탈퇴한 선수 엔트리를 제거한다")
        fun `should remove player entry from DRAFT lineup submission`() {
            // given
            val event =
                TeamMemberLeftEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            val submission = mockk<LineupSubmission>(relaxed = true)
            every { submission.id } returns 10L
            every { submission.status } returns LineupSubmissionStatus.DRAFT

            val entry = mockk<LineupEntry>(relaxed = true)

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED,
                    ),
                )
            } returns listOf(submission)
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(10L, 20L) } returns entry
            justRun { lineupEntryRepository.delete(entry) }
            every { lineupEntryRepository.findAllBySubmissionId(10L) } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 1) { lineupEntryRepository.delete(entry) }
            verify(exactly = 1) { activityScoreRepository.deleteByMemberId(100L) }
        }

        @Test
        @DisplayName("해당 선수가 라인업에 없으면 아무 처리도 하지 않는다")
        fun `should do nothing when player is not in lineup`() {
            // given
            val event =
                TeamMemberLeftEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            val submission = mockk<LineupSubmission>(relaxed = true)
            every { submission.id } returns 10L
            every { submission.status } returns LineupSubmissionStatus.DRAFT

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns listOf(submission)
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(10L, 20L) } returns null
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 0) { lineupEntryRepository.delete(any()) }
            verify(exactly = 1) { activityScoreRepository.deleteByMemberId(100L) }
        }

        @Test
        @DisplayName("활성 라인업이 없으면 아무 처리도 하지 않는다")
        fun `should do nothing when no active lineup submissions exist`() {
            // given
            val event =
                TeamMemberLeftEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 0) { lineupEntryRepository.findBySubmissionIdAndPlayerId(any(), any()) }
            verify(exactly = 0) { lineupEntryRepository.delete(any()) }
            verify(exactly = 1) { activityScoreRepository.deleteByMemberId(100L) }
        }

        @Test
        @DisplayName("SUBMITTED 라인업에서 마지막 선수 제거 시 경고 로그를 남긴다")
        fun `should log warning when last entry removed from SUBMITTED lineup`() {
            // given
            val event =
                TeamMemberLeftEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            val submission = mockk<LineupSubmission>(relaxed = true)
            every { submission.id } returns 10L
            every { submission.status } returns LineupSubmissionStatus.SUBMITTED

            val entry = mockk<LineupEntry>(relaxed = true)

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns listOf(submission)
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(10L, 20L) } returns entry
            justRun { lineupEntryRepository.delete(entry) }
            every { lineupEntryRepository.findAllBySubmissionId(10L) } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 1) { lineupEntryRepository.delete(entry) }
            verify(exactly = 1) { lineupEntryRepository.findAllBySubmissionId(10L) }
            verify(exactly = 1) { activityScoreRepository.deleteByMemberId(100L) }
        }

        @Test
        @DisplayName("팀원 탈퇴 시 활동 점수를 삭제한다")
        fun `should delete activity score on member left`() {
            // given
            val event =
                TeamMemberLeftEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 1) { activityScoreRepository.deleteByMemberId(100L) }
        }

        @Test
        @DisplayName("팀원 탈퇴 시 활성 CompetitionPlayer를 WITHDRAWN 처리한다")
        fun `should withdraw active competition players on member left`() {
            // given
            val event =
                TeamMemberLeftEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")
            val competitionPlayer = mockk<CompetitionPlayer>(relaxed = true)
            every { competitionPlayer.id } returns 50L

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns listOf(competitionPlayer)
            justRun { competitionPlayer.withdraw() }
            every { competitionPlayerRepository.save(competitionPlayer) } returns competitionPlayer
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 1) { competitionPlayer.withdraw() }
            verify(exactly = 1) { competitionPlayerRepository.save(competitionPlayer) }
        }

        @Test
        @DisplayName("CONFIRMED 라인업에서도 탈퇴한 선수 엔트리를 제거한다")
        fun `should remove player entry from CONFIRMED lineup submission`() {
            // given
            val event =
                TeamMemberLeftEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            val submission = mockk<LineupSubmission>(relaxed = true)
            every { submission.id } returns 10L
            every { submission.status } returns LineupSubmissionStatus.CONFIRMED

            val entry = mockk<LineupEntry>(relaxed = true)

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns listOf(submission)
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(10L, 20L) } returns entry
            justRun { lineupEntryRepository.delete(entry) }
            every { lineupEntryRepository.findAllBySubmissionId(10L) } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 1) { lineupEntryRepository.delete(entry) }
        }
    }

    // -------------------------------------------------------------------------
    // TeamMemberKickedEvent 처리 테스트
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleTeamMemberKicked")
    inner class HandleTeamMemberKicked {
        @Test
        @DisplayName("SUBMITTED 라인업에서 강퇴된 선수 엔트리를 제거한다")
        fun `should remove player entry from SUBMITTED lineup submission`() {
            // given
            val event =
                TeamMemberKickedEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            val submission = mockk<LineupSubmission>(relaxed = true)
            every { submission.id } returns 10L
            every { submission.status } returns LineupSubmissionStatus.SUBMITTED

            val entry = mockk<LineupEntry>(relaxed = true)
            val remainingEntry = mockk<LineupEntry>(relaxed = true)

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns listOf(submission)
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(10L, 20L) } returns entry
            justRun { lineupEntryRepository.delete(entry) }
            every { lineupEntryRepository.findAllBySubmissionId(10L) } returns listOf(remainingEntry)
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberKicked(event)

            // then
            verify(exactly = 1) { lineupEntryRepository.delete(entry) }
            verify(exactly = 1) { activityScoreRepository.deleteByMemberId(100L) }
        }

        @Test
        @DisplayName("여러 라인업에 선수가 있으면 모두 제거한다")
        fun `should remove player entry from multiple lineup submissions`() {
            // given
            val event =
                TeamMemberKickedEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            val submission1 = mockk<LineupSubmission>(relaxed = true)
            every { submission1.id } returns 10L
            every { submission1.status } returns LineupSubmissionStatus.DRAFT

            val submission2 = mockk<LineupSubmission>(relaxed = true)
            every { submission2.id } returns 11L
            every { submission2.status } returns LineupSubmissionStatus.SUBMITTED

            val entry1 = mockk<LineupEntry>(relaxed = true)
            val entry2 = mockk<LineupEntry>(relaxed = true)

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns listOf(submission1, submission2)
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(10L, 20L) } returns entry1
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(11L, 20L) } returns entry2
            justRun { lineupEntryRepository.delete(entry1) }
            justRun { lineupEntryRepository.delete(entry2) }
            every { lineupEntryRepository.findAllBySubmissionId(10L) } returns emptyList()
            every { lineupEntryRepository.findAllBySubmissionId(11L) } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberKicked(event)

            // then
            verify(exactly = 1) { lineupEntryRepository.delete(entry1) }
            verify(exactly = 1) { lineupEntryRepository.delete(entry2) }
            verify(exactly = 1) { activityScoreRepository.deleteByMemberId(100L) }
        }

        @Test
        @DisplayName("팀원 강퇴 시 활동 점수를 삭제한다")
        fun `should delete activity score on member kicked`() {
            // given
            val event =
                TeamMemberKickedEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns emptyList()
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberKicked(event)

            // then
            verify(exactly = 1) { activityScoreRepository.deleteByMemberId(100L) }
        }

        @Test
        @DisplayName("팀원 강퇴 시 활성 CompetitionPlayer를 WITHDRAWN 처리한다")
        fun `should withdraw active competition players on member kicked`() {
            // given
            val event =
                TeamMemberKickedEvent(teamId = 1L, userId = 10L, playerId = 20L, memberId = 100L, teamName = "타이거즈")
            val competitionPlayer = mockk<CompetitionPlayer>(relaxed = true)
            every { competitionPlayer.id } returns 50L

            every {
                lineupSubmissionRepository.findAllByTeamIdAndStatusIn(
                    1L,
                    listOf(
                        LineupSubmissionStatus.DRAFT,
                        LineupSubmissionStatus.SUBMITTED,
                        LineupSubmissionStatus.CONFIRMED
                    ),
                )
            } returns emptyList()
            every {
                competitionPlayerRepository.findByPlayerIdAndStatusIn(
                    20L,
                    listOf(CompetitionPlayerStatus.ACTIVE, CompetitionPlayerStatus.SUSPENDED),
                )
            } returns listOf(competitionPlayer)
            justRun { competitionPlayer.withdraw() }
            every { competitionPlayerRepository.save(competitionPlayer) } returns competitionPlayer
            justRun { activityScoreRepository.deleteByMemberId(100L) }

            // when
            listener.handleTeamMemberKicked(event)

            // then
            verify(exactly = 1) { competitionPlayer.withdraw() }
            verify(exactly = 1) { competitionPlayerRepository.save(competitionPlayer) }
        }
    }

    // -------------------------------------------------------------------------
    // TeamDisbandedEvent 처리 테스트
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleTeamDisbanded")
    inner class HandleTeamDisbanded {
        private fun stubEmptyDisbandDefaults() {
            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns emptySet()
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }
        }

        @Test
        @DisplayName("팀 해산 시 활성 CompetitionPlayer를 WITHDRAWN 처리한다")
        fun `should withdraw active competition players on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)
            val competitionPlayer = mockk<CompetitionPlayer>(relaxed = true)
            every { competitionPlayer.id } returns 50L

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns listOf(competitionPlayer)
            justRun { competitionPlayer.withdraw() }
            every { competitionPlayerRepository.save(competitionPlayer) } returns competitionPlayer

            // 대회가 IN_PROGRESS가 아닌 경우 (몰수승 처리 안 함)
            val competition = mockk<Competition>(relaxed = true)
            every { competition.status } returns CompetitionStatus.SCHEDULED
            every { competitionRepository.findByIdOrNull(100L) } returns competition

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 1) { competitionPlayer.withdraw() }
            verify(exactly = 1) { competitionPlayerRepository.save(competitionPlayer) }
        }

        @Test
        @DisplayName("팀 해산 시 CONFIRMED 구장 예약을 취소한다")
        fun `should cancel confirmed stadium bookings on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)
            val booking = mockk<StadiumBooking>(relaxed = true)
            every { booking.id } returns 60L

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns emptySet()
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every {
                stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED)
            } returns listOf(booking)
            justRun { booking.cancel() }
            every { stadiumBookingRepository.save(booking) } returns booking
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 1) { booking.cancel() }
            verify(exactly = 1) { stadiumBookingRepository.save(booking) }
        }

        @Test
        @DisplayName("팀 해산 시 OPEN 출석 투표를 마감한다")
        fun `should close open attendance polls on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)
            val poll = mockk<AttendancePoll>(relaxed = true)
            every { poll.id } returns 70L

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns emptySet()
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns listOf(poll)
            justRun { poll.close() }
            every { attendancePollRepository.save(poll) } returns poll
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 1) { poll.close() }
            verify(exactly = 1) { attendancePollRepository.save(poll) }
        }

        @Test
        @DisplayName("팀 해산 시 SCHEDULED/IN_PROGRESS 선거를 취소한다")
        fun `should cancel scheduled and in-progress elections on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val scheduledElection =
                Election.create(
                    teamId = 1L,
                    title = "예정 선거",
                    electionType = ElectionType.OWNER_ELECTION,
                    startAt = Instant.now().plusSeconds(3600),
                    endAt = Instant.now().plusSeconds(7200),
                )
            val inProgressElection =
                Election.createEmergency(
                    teamId = 1L,
                    triggeredByMemberId = 100L,
                    title = "긴급 선거",
                    startAt = Instant.now().minusSeconds(3600),
                    endAt = Instant.now().plusSeconds(3600),
                )
            val completedElection =
                Election.create(
                    teamId = 1L,
                    title = "완료된 선거",
                    electionType = ElectionType.CAPTAIN_ELECTION,
                    startAt = Instant.now().minusSeconds(7200),
                    endAt = Instant.now().minusSeconds(3600),
                ).also {
                    it.start()
                    it.complete()
                }

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns emptySet()
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns
                listOf(scheduledElection, inProgressElection, completedElection)
            every { electionRepository.save(any()) } returnsArgument 0
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 2) { electionRepository.save(any()) }
            assert(scheduledElection.status == ElectionStatus.CANCELLED)
            assert(inProgressElection.status == ElectionStatus.CANCELLED)
        }

        @Test
        @DisplayName("팀 해산 시 PENDING 가입 신청을 일괄 거절한다")
        fun `should reject pending join requests on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val pendingRequest = mockk<TeamJoinRequest>(relaxed = true)
            every { pendingRequest.id } returns 80L
            every { pendingRequest.status } returns JoinRequestStatus.PENDING

            val approvedRequest = mockk<TeamJoinRequest>(relaxed = true)
            every { approvedRequest.status } returns JoinRequestStatus.APPROVED

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns emptySet()
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns listOf(pendingRequest, approvedRequest)
            justRun { pendingRequest.cancel() }
            every { teamJoinRequestRepository.save(pendingRequest) } returns pendingRequest
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 1) { pendingRequest.cancel() }
            verify(exactly = 1) { teamJoinRequestRepository.save(pendingRequest) }
            verify(exactly = 0) { approvedRequest.cancel() }
        }

        @Test
        @DisplayName("팀 해산 시 처리 대상이 없으면 저장 호출이 없다")
        fun `should not call save when no entities to process on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)
            stubEmptyDisbandDefaults()

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 0) { competitionPlayerRepository.save(any()) }
            verify(exactly = 0) { stadiumBookingRepository.save(any()) }
            verify(exactly = 0) { attendancePollRepository.save(any()) }
            verify(exactly = 0) { electionRepository.save(any()) }
            verify(exactly = 0) { teamJoinRequestRepository.save(any()) }
            verify(exactly = 0) { gameRepository.save(any()) }
            verify(exactly = 0) { bracketEntryRepository.save(any()) }
        }

        @Test
        @DisplayName("팀 해산 시 팀의 모든 활동 점수를 삭제한다")
        fun `should delete all activity scores for team on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)
            stubEmptyDisbandDefaults()

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 1) { activityScoreRepository.deleteByTeamId(1L) }
        }

        @Test
        @DisplayName("팀 해산 시 진행 중 대회의 잔여 경기를 몰수승 처리한다")
        fun `should forfeit remaining games in in-progress competition on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val opponentTeam = Team(league = league, name = "라이언즈", city = "부산", foundedYear = 2016)
            setFieldId(opponentTeam, Team::class.java, 2L)

            val competition =
                Competition(
                    league = league,
                    name = "2026 시즌",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition, Competition::class.java, 100L)
            competition.start()

            // SCHEDULED 경기 - 몰수승 처리 대상
            val scheduledGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = team,
                    awayTeam = opponentTeam,
                    scheduledAt = LocalDateTime.now().plusDays(1),
                    status = GameStatus.SCHEDULED,
                    id = 1000L,
                )

            // IN_PROGRESS 경기 - 몰수승 처리 대상
            val inProgressGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = opponentTeam,
                    awayTeam = team,
                    scheduledAt = LocalDateTime.now(),
                    status = GameStatus.IN_PROGRESS,
                    currentInning = 3,
                    id = 1001L,
                )

            // FINISHED 경기 - 몰수승 처리 대상이 아님
            val finishedGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = team,
                    awayTeam = opponentTeam,
                    scheduledAt = LocalDateTime.now().minusDays(1),
                    status = GameStatus.FINISHED,
                    id = 1002L,
                )

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionRepository.findByIdOrNull(100L) } returns competition
            every { gameRepository.findByCompetitionId(100L) } returns
                listOf(scheduledGame, inProgressGame, finishedGame)
            every { gameRepository.save(any()) } returnsArgument 0
            every { bracketEntryRepository.findByCompetitionId(100L) } returns emptyList()

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 2) { gameRepository.save(any()) }
            assert(scheduledGame.status == GameStatus.FORFEITED)
            assert(inProgressGame.status == GameStatus.FORFEITED)
            assert(finishedGame.status == GameStatus.FINISHED)
        }

        @Test
        @DisplayName("팀 해산 몰수승 처리 후 GameResultConfirmedEvent가 발행된다")
        fun `should publish GameResultConfirmedEvent after forfeit on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val opponentTeam = Team(league = league, name = "라이언즈", city = "부산", foundedYear = 2016)
            setFieldId(opponentTeam, Team::class.java, 2L)

            val competition =
                Competition(
                    league = league,
                    name = "2026 시즌",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition, Competition::class.java, 100L)
            competition.start()

            val scheduledGame2 =
                Game.createForTest(
                    competition = competition,
                    homeTeam = team,
                    awayTeam = opponentTeam,
                    scheduledAt = LocalDateTime.now().plusDays(1),
                    status = GameStatus.SCHEDULED,
                    id = 1000L,
                )

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionRepository.findByIdOrNull(100L) } returns competition
            every { gameRepository.findByCompetitionId(100L) } returns listOf(scheduledGame2)
            every { gameRepository.save(any()) } returnsArgument 0
            every { bracketEntryRepository.findByCompetitionId(100L) } returns emptyList()

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            val eventSlot = slot<GameResultConfirmedEvent>()
            verify { eventPublisher.publishEvent(capture(eventSlot)) }

            val capturedEvent = eventSlot.captured
            assert(capturedEvent.gameId == 1000L)
        }

        @Test
        @DisplayName("대회 미참가 팀 해산 시 에러 없이 통과한다")
        fun `should pass without error when team is not in any competition`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns emptySet()
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 0) { gameRepository.findByCompetitionId(any()) }
            verify(exactly = 0) { gameRepository.save(any()) }
            verify(exactly = 0) { bracketEntryRepository.findByCompetitionId(any()) }
        }

        @Test
        @DisplayName("여러 대회에 참가 중인 팀 해산 시 모든 대회에서 탈퇴하고 몰수승 처리한다")
        fun `should withdraw from all competitions and forfeit games when team in multiple competitions`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val opponentTeam1 = Team(league = league, name = "라이언즈", city = "부산", foundedYear = 2016)
            setFieldId(opponentTeam1, Team::class.java, 2L)

            val opponentTeam2 = Team(league = league, name = "이글스", city = "대전", foundedYear = 2017)
            setFieldId(opponentTeam2, Team::class.java, 3L)

            // 대회 1 - IN_PROGRESS
            val competition1 =
                Competition(
                    league = league,
                    name = "2026 시즌 1",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition1, Competition::class.java, 100L)
            competition1.start()

            // 대회 2 - IN_PROGRESS
            val competition2 =
                Competition(
                    league = league,
                    name = "2026 시즌 2",
                    year = 2026,
                    season = 2,
                    type = CompetitionType.TOURNAMENT,
                    startDate = LocalDate.of(2026, 6, 1),
                )
            setFieldId(competition2, Competition::class.java, 200L)
            competition2.start()

            val game1 =
                Game.createForTest(
                    competition = competition1,
                    homeTeam = team,
                    awayTeam = opponentTeam1,
                    scheduledAt = LocalDateTime.now().plusDays(1),
                    status = GameStatus.SCHEDULED,
                    id = 1000L,
                )

            val game2 =
                Game.createForTest(
                    competition = competition2,
                    homeTeam = opponentTeam2,
                    awayTeam = team,
                    scheduledAt = LocalDateTime.now().plusDays(2),
                    status = GameStatus.POSTPONED,
                    id = 2000L,
                )

            val cp1 = mockk<CompetitionPlayer>(relaxed = true)
            every { cp1.id } returns 50L
            val cp2 = mockk<CompetitionPlayer>(relaxed = true)
            every { cp2.id } returns 51L

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L, 200L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns listOf(cp1, cp2)
            every { competitionPlayerRepository.save(any()) } returnsArgument 0

            every { competitionRepository.findByIdOrNull(100L) } returns competition1
            every { competitionRepository.findByIdOrNull(200L) } returns competition2
            every { gameRepository.findByCompetitionId(100L) } returns listOf(game1)
            every { gameRepository.findByCompetitionId(200L) } returns listOf(game2)
            every { gameRepository.save(any()) } returnsArgument 0
            every { bracketEntryRepository.findByCompetitionId(100L) } returns emptyList()
            every { bracketEntryRepository.findByCompetitionId(200L) } returns emptyList()

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 2) { competitionPlayerRepository.save(any()) }
            verify(exactly = 2) { gameRepository.save(any()) }
            assert(game1.status == GameStatus.FORFEITED)
            assert(game2.status == GameStatus.FORFEITED)
        }

        @Test
        @DisplayName("팀 해산 시 IN_PROGRESS가 아닌 대회에서는 몰수승 처리를 하지 않는다")
        fun `should not forfeit games for non-in-progress competitions`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val cp = mockk<CompetitionPlayer>(relaxed = true)
            every { cp.id } returns 50L

            // SCHEDULED 상태 대회
            val competition = mockk<Competition>(relaxed = true)
            every { competition.status } returns CompetitionStatus.SCHEDULED

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns listOf(cp)
            every { competitionPlayerRepository.save(any()) } returnsArgument 0
            every { competitionRepository.findByIdOrNull(100L) } returns competition

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            // CompetitionPlayer는 WITHDRAWN 처리되지만 경기 몰수승은 안 함
            verify(exactly = 1) { competitionPlayerRepository.save(any()) }
            verify(exactly = 0) { gameRepository.findByCompetitionId(any()) }
            verify(exactly = 0) { gameRepository.save(any()) }
        }

        @Test
        @DisplayName("팀 해산 시 대진표에서 team1이 해산팀이면 team2가 부전승 처리된다")
        fun `should give walkover win to team2 when team1 is disbanded team in bracket`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val opponentTeam =
                Team(league = league, name = "라이언즈", city = "부산", foundedYear = 2016)
            setFieldId(opponentTeam, Team::class.java, 2L)

            val competition =
                Competition(
                    league = league,
                    name = "2026 토너먼트",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.TOURNAMENT,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition, Competition::class.java, 100L)
            competition.start()

            // team1이 해산팀(id=1L), team2가 상대팀
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = team,
                    team2 = opponentTeam,
                )
            setFieldId(bracketEntry, BracketEntry::class.java, 500L)

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionRepository.findByIdOrNull(100L) } returns competition
            every { gameRepository.findByCompetitionId(100L) } returns emptyList()
            every { bracketEntryRepository.findByCompetitionId(100L) } returns listOf(bracketEntry)
            every { bracketEntryRepository.save(any()) } returnsArgument 0

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 1) { bracketEntryRepository.save(bracketEntry) }
            assert(bracketEntry.winner == opponentTeam)
        }

        @Test
        @DisplayName("팀 해산 시 대진표에서 team2가 해산팀이면 team1이 부전승 처리된다")
        fun `should give walkover win to team1 when team2 is disbanded team in bracket`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val opponentTeam =
                Team(league = league, name = "라이언즈", city = "부산", foundedYear = 2016)
            setFieldId(opponentTeam, Team::class.java, 2L)

            val competition =
                Competition(
                    league = league,
                    name = "2026 토너먼트",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.TOURNAMENT,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition, Competition::class.java, 100L)
            competition.start()

            // team1이 상대팀, team2가 해산팀(id=1L)
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = opponentTeam,
                    team2 = team,
                )
            setFieldId(bracketEntry, BracketEntry::class.java, 501L)

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionRepository.findByIdOrNull(100L) } returns competition
            every { gameRepository.findByCompetitionId(100L) } returns emptyList()
            every { bracketEntryRepository.findByCompetitionId(100L) } returns listOf(bracketEntry)
            every { bracketEntryRepository.save(any()) } returnsArgument 0

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 1) { bracketEntryRepository.save(bracketEntry) }
            assert(bracketEntry.winner == opponentTeam)
        }

        @Test
        @DisplayName("이미 완료된 대진표 엔트리는 부전승 처리하지 않는다")
        fun `should skip completed bracket entries`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val opponentTeam =
                Team(league = league, name = "라이언즈", city = "부산", foundedYear = 2016)
            setFieldId(opponentTeam, Team::class.java, 2L)

            val competition =
                Competition(
                    league = league,
                    name = "2026 토너먼트",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.TOURNAMENT,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition, Competition::class.java, 100L)
            competition.start()

            // 이미 승자가 결정된 대진표 엔트리
            val completedEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = team,
                    team2 = opponentTeam,
                    winner = opponentTeam,
                )
            setFieldId(completedEntry, BracketEntry::class.java, 502L)

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionRepository.findByIdOrNull(100L) } returns competition
            every { gameRepository.findByCompetitionId(100L) } returns emptyList()
            every { bracketEntryRepository.findByCompetitionId(100L) } returns listOf(completedEntry)

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 0) { bracketEntryRepository.save(any()) }
        }

        @Test
        @DisplayName("대진표에서 상대팀이 null이면 부전승 처리하지 않는다")
        fun `should not process bracket entry when opponent team is null`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val competition =
                Competition(
                    league = league,
                    name = "2026 토너먼트",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.TOURNAMENT,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition, Competition::class.java, 100L)
            competition.start()

            // team1이 해산팀이고 team2가 null (이미 bye 상태이므로 isCompleted=true)
            val byeEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = team,
                    team2 = null,
                )
            setFieldId(byeEntry, BracketEntry::class.java, 503L)

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionRepository.findByIdOrNull(100L) } returns competition
            every { gameRepository.findByCompetitionId(100L) } returns emptyList()
            every { bracketEntryRepository.findByCompetitionId(100L) } returns listOf(byeEntry)

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 0) { bracketEntryRepository.save(any()) }
        }

        @Test
        @DisplayName("대회가 조회되지 않으면 몰수승/부전승 처리를 건너뛴다")
        fun `should skip forfeit when competition not found`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            // 대회 조회 시 null 반환
            every { competitionRepository.findByIdOrNull(100L) } returns null

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 0) { gameRepository.findByCompetitionId(any()) }
            verify(exactly = 0) { gameRepository.save(any()) }
            verify(exactly = 0) { bracketEntryRepository.findByCompetitionId(any()) }
            verify(exactly = 0) { bracketEntryRepository.save(any()) }
        }

        @Test
        @DisplayName("경기에 해산팀이 포함되지 않으면 몰수승 처리하지 않는다")
        fun `should not forfeit game when disbanded team is not in the game`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val otherTeam1 =
                Team(league = league, name = "라이언즈", city = "부산", foundedYear = 2016)
            setFieldId(otherTeam1, Team::class.java, 2L)

            val otherTeam2 =
                Team(league = league, name = "이글스", city = "대전", foundedYear = 2017)
            setFieldId(otherTeam2, Team::class.java, 3L)

            val competition =
                Competition(
                    league = league,
                    name = "2026 시즌",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition, Competition::class.java, 100L)
            competition.start()

            // 해산팀(teamId=1L)이 포함되지 않은 경기
            val gameNotInvolvingTeam =
                Game.createForTest(
                    competition = competition,
                    homeTeam = otherTeam1,
                    awayTeam = otherTeam2,
                    scheduledAt = LocalDateTime.now().plusDays(1),
                    status = GameStatus.SCHEDULED,
                    id = 1000L,
                )

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionRepository.findByIdOrNull(100L) } returns competition
            every { gameRepository.findByCompetitionId(100L) } returns listOf(gameNotInvolvingTeam)
            every { bracketEntryRepository.findByCompetitionId(100L) } returns emptyList()

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 0) { gameRepository.save(any()) }
            assert(gameNotInvolvingTeam.status == GameStatus.SCHEDULED)
        }

        @Test
        @DisplayName("대진표에 해산팀과 무관한 엔트리는 부전승 처리하지 않는다")
        fun `should not process bracket entry when disbanded team is not involved`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

            val otherTeam1 =
                Team(league = league, name = "라이언즈", city = "부산", foundedYear = 2016)
            setFieldId(otherTeam1, Team::class.java, 2L)

            val otherTeam2 =
                Team(league = league, name = "이글스", city = "대전", foundedYear = 2017)
            setFieldId(otherTeam2, Team::class.java, 3L)

            val competition =
                Competition(
                    league = league,
                    name = "2026 토너먼트",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.TOURNAMENT,
                    startDate = LocalDate.of(2026, 3, 1),
                )
            setFieldId(competition, Competition::class.java, 100L)
            competition.start()

            // 해산팀과 무관한 대진표 엔트리
            val unrelatedEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = otherTeam1,
                    team2 = otherTeam2,
                )
            setFieldId(unrelatedEntry, BracketEntry::class.java, 504L)

            every {
                competitionPlayerRepository.findActiveCompetitionIdsByTeamId(1L)
            } returns setOf(100L)
            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionRepository.findByIdOrNull(100L) } returns competition
            every { gameRepository.findByCompetitionId(100L) } returns emptyList()
            every { bracketEntryRepository.findByCompetitionId(100L) } returns listOf(unrelatedEntry)

            every { stadiumBookingRepository.findByTeamIdAndStatus(1L, BookingStatus.CONFIRMED) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { teamJoinRequestRepository.findByTeamId(1L) } returns emptyList()
            justRun { activityScoreRepository.deleteByTeamId(1L) }

            // when
            listener.handleTeamDisbanded(event)

            // then
            verify(exactly = 0) { bracketEntryRepository.save(any()) }
            assert(unrelatedEntry.winner == null)
        }
    }

    private fun setFieldId(
        entity: Any,
        clazz: Class<*>,
        id: Long,
    ) {
        val idField = clazz.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
