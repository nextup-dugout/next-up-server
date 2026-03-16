package com.nextup.infrastructure.listener

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionStatus
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.event.TeamDisbandedEvent
import com.nextup.core.domain.event.TeamMemberKickedEvent
import com.nextup.core.domain.event.TeamMemberLeftEvent
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
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.LineupEntryRepositoryPort
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import com.nextup.core.port.repository.TeamJoinRequestRepositoryPort
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("TeamMemberEventListener")
class TeamMemberEventListenerTest {
    private val lineupSubmissionRepository = mockk<LineupSubmissionRepositoryPort>()
    private val lineupEntryRepository = mockk<LineupEntryRepositoryPort>()
    private val competitionPlayerRepository = mockk<CompetitionPlayerRepositoryPort>()
    private val stadiumBookingRepository = mockk<StadiumBookingRepositoryPort>()
    private val attendancePollRepository = mockk<AttendancePollRepositoryPort>()
    private val electionRepository = mockk<ElectionRepositoryPort>()
    private val teamJoinRequestRepository = mockk<TeamJoinRequestRepositoryPort>()
    private val activityScoreRepository = mockk<ActivityScoreRepositoryPort>()

    private val listener =
        TeamMemberEventListener(
            lineupSubmissionRepository = lineupSubmissionRepository,
            lineupEntryRepository = lineupEntryRepository,
            competitionPlayerRepository = competitionPlayerRepository,
            stadiumBookingRepository = stadiumBookingRepository,
            attendancePollRepository = attendancePollRepository,
            electionRepository = electionRepository,
            teamJoinRequestRepository = teamJoinRequestRepository,
            activityScoreRepository = activityScoreRepository,
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
            val event = TeamMemberLeftEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberLeftEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberLeftEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberLeftEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberLeftEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberLeftEvent(teamId = 1L, playerId = 20L, memberId = 100L)
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
            val event = TeamMemberLeftEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberKickedEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberKickedEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberKickedEvent(teamId = 1L, playerId = 20L, memberId = 100L)

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
            val event = TeamMemberKickedEvent(teamId = 1L, playerId = 20L, memberId = 100L)
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
        @Test
        @DisplayName("팀 해산 시 활성 CompetitionPlayer를 WITHDRAWN 처리한다")
        fun `should withdraw active competition players on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)
            val competitionPlayer = mockk<CompetitionPlayer>(relaxed = true)
            every { competitionPlayer.id } returns 50L

            every {
                competitionPlayerRepository.findByTeamIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns listOf(competitionPlayer)
            justRun { competitionPlayer.withdraw() }
            every { competitionPlayerRepository.save(competitionPlayer) } returns competitionPlayer

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
            verify(exactly = 0) { competitionPlayerRepository.save(any()) }
            verify(exactly = 0) { stadiumBookingRepository.save(any()) }
            verify(exactly = 0) { attendancePollRepository.save(any()) }
            verify(exactly = 0) { electionRepository.save(any()) }
            verify(exactly = 0) { teamJoinRequestRepository.save(any()) }
        }

        @Test
        @DisplayName("팀 해산 시 팀의 모든 활동 점수를 삭제한다")
        fun `should delete all activity scores for team on team disband`() {
            // given
            val event = TeamDisbandedEvent(teamId = 1L)

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
            verify(exactly = 1) { activityScoreRepository.deleteByTeamId(1L) }
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
