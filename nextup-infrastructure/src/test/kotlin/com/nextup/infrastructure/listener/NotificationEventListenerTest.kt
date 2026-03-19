package com.nextup.infrastructure.listener

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.event.AttendanceVoteCreatedEvent
import com.nextup.core.domain.event.ElectionTiedEvent
import com.nextup.core.domain.event.GameCancelledEvent
import com.nextup.core.domain.event.GamePostponedEvent
import com.nextup.core.domain.event.GameRescheduledEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.LineupConfirmedEvent
import com.nextup.core.domain.event.TeamJoinApprovedEvent
import com.nextup.core.domain.event.TeamJoinRejectedEvent
import com.nextup.core.domain.event.TeamMemberKickedEvent
import com.nextup.core.domain.event.TeamMemberLeftEvent
import com.nextup.core.domain.league.League
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.notification.NotificationService
import com.nextup.core.service.notification.dto.SendNotificationRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("NotificationEventListener")
class NotificationEventListenerTest {
    private lateinit var notificationService: NotificationService
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var listener: NotificationEventListener

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var team: Team
    private lateinit var user: User
    private lateinit var player: Player
    private lateinit var member: TeamMember

    @BeforeEach
    fun setUp() {
        notificationService = mockk(relaxed = true)
        teamMemberRepository = mockk()

        listener =
            NotificationEventListener(
                notificationService = notificationService,
                teamMemberRepository = teamMemberRepository,
            )

        association = Association(name = "서울시야구협회", region = "서울")
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

    @Nested
    @DisplayName("handleTeamJoinApproved")
    inner class HandleTeamJoinApproved {
        @Test
        fun `should send TEAM_JOIN_APPROVED notification to the user`() {
            // given
            val event =
                TeamJoinApprovedEvent(
                    teamId = 1L,
                    userId = 10L,
                    teamName = "타이거즈",
                )
            val requestSlot = slot<SendNotificationRequest>()
            every { notificationService.sendNotification(capture(requestSlot)) } returns mockk()

            // when
            listener.handleTeamJoinApproved(event)

            // then
            verify(exactly = 1) { notificationService.sendNotification(any()) }
            assertThat(requestSlot.captured.userId).isEqualTo(10L)
            assertThat(requestSlot.captured.type).isEqualTo(NotificationType.TEAM_JOIN_APPROVED)
            assertThat(requestSlot.captured.body).contains("타이거즈")
        }
    }

    @Nested
    @DisplayName("handleTeamJoinRejected")
    inner class HandleTeamJoinRejected {
        @Test
        fun `should send TEAM_JOIN_REJECTED notification to the user`() {
            // given
            val event =
                TeamJoinRejectedEvent(
                    teamId = 1L,
                    userId = 10L,
                    teamName = "타이거즈",
                )
            val requestSlot = slot<SendNotificationRequest>()
            every { notificationService.sendNotification(capture(requestSlot)) } returns mockk()

            // when
            listener.handleTeamJoinRejected(event)

            // then
            verify(exactly = 1) { notificationService.sendNotification(any()) }
            assertThat(requestSlot.captured.userId).isEqualTo(10L)
            assertThat(requestSlot.captured.type).isEqualTo(NotificationType.TEAM_JOIN_REJECTED)
            assertThat(requestSlot.captured.body).contains("타이거즈")
        }
    }

    @Nested
    @DisplayName("handleAttendanceVoteCreated")
    inner class HandleAttendanceVoteCreated {
        @Test
        fun `should send ATTENDANCE_VOTE_CREATED notification to all active team members via batch`() {
            // given
            val eventDate = LocalDateTime.of(2026, 3, 15, 10, 0)
            val event =
                AttendanceVoteCreatedEvent(
                    teamId = 1L,
                    pollId = 5L,
                    eventDate = eventDate,
                )
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(member)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleAttendanceVoteCreated(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(1)
            assertThat(requestsSlot.captured[0].userId).isEqualTo(10L)
            assertThat(requestsSlot.captured[0].type).isEqualTo(NotificationType.ATTENDANCE_VOTE_CREATED)
        }

        @Test
        fun `should send notification to each active member when team has multiple members`() {
            // given
            val user2 = User.createLocalUser("user2@example.com", "password", "유저2")
            setFieldId(user2, User::class.java, 11L)
            val player2 = Player(name = "유저2", primaryPosition = Position.CATCHER)
            setFieldId(player2, Player::class.java, 21L)
            user2.player = player2
            val member2 = TeamMember.create(team, user2, player2, 8, TeamMemberRole.MEMBER)

            val eventDate = LocalDateTime.of(2026, 3, 15, 10, 0)
            val event =
                AttendanceVoteCreatedEvent(
                    teamId = 1L,
                    pollId = 5L,
                    eventDate = eventDate,
                )
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(member, member2)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleAttendanceVoteCreated(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(2)
        }

        @Test
        fun `should not send notification when team has no active members`() {
            // given
            val event =
                AttendanceVoteCreatedEvent(
                    teamId = 1L,
                    pollId = 5L,
                    eventDate = LocalDateTime.now().plusDays(7),
                )
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns emptyList()

            // when
            listener.handleAttendanceVoteCreated(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(emptyList()) }
        }
    }

    @Nested
    @DisplayName("handleGameResultConfirmed")
    inner class HandleGameResultConfirmed {
        @Test
        fun `should send GAME_RESULT_CONFIRMED notification to both teams members via batch`() {
            // given
            val homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2015)
            setFieldId(homeTeam, Team::class.java, 2L)
            val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2015)
            setFieldId(awayTeam, Team::class.java, 3L)

            val homeUser = User.createLocalUser("home@example.com", "pw", "홈멤버")
            setFieldId(homeUser, User::class.java, 30L)
            val homePlayer = Player(name = "홈멤버", primaryPosition = Position.SHORTSTOP)
            homeUser.player = homePlayer
            val homeMember = TeamMember.create(homeTeam, homeUser, homePlayer, 1, TeamMemberRole.MEMBER)

            val awayUser = User.createLocalUser("away@example.com", "pw", "원정멤버")
            setFieldId(awayUser, User::class.java, 31L)
            val awayPlayer = Player(name = "원정멤버", primaryPosition = Position.CATCHER)
            awayUser.player = awayPlayer
            val awayMember = TeamMember.create(awayTeam, awayUser, awayPlayer, 2, TeamMemberRole.MEMBER)

            val event =
                GameResultConfirmedEvent(
                    gameId = 100L,
                    homeTeamId = 2L,
                    awayTeamId = 3L,
                    homeScore = 5,
                    awayScore = 3,
                )

            every {
                teamMemberRepository.findByTeamIdInAndStatus(listOf(2L, 3L), TeamMemberStatus.ACTIVE)
            } returns listOf(homeMember, awayMember)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleGameResultConfirmed(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(2)
            assertThat(requestsSlot.captured.map { it.userId }).containsExactlyInAnyOrder(30L, 31L)
            assertThat(requestsSlot.captured.all { it.type == NotificationType.GAME_RESULT_CONFIRMED }).isTrue()
            assertThat(requestsSlot.captured[0].body).contains("5").contains("3")
        }
    }

    @Nested
    @DisplayName("handleLineupConfirmed")
    inner class HandleLineupConfirmed {
        @Test
        fun `should send LINEUP_CONFIRMED notification to all active team members via batch`() {
            // given
            val event =
                LineupConfirmedEvent(
                    gameId = 100L,
                    teamId = 1L,
                )
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(member)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleLineupConfirmed(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(1)
            assertThat(requestsSlot.captured[0].userId).isEqualTo(10L)
            assertThat(requestsSlot.captured[0].type).isEqualTo(NotificationType.LINEUP_CONFIRMED)
        }

        @Test
        fun `should not send notification when no active members`() {
            // given
            val event = LineupConfirmedEvent(gameId = 100L, teamId = 1L)
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns emptyList()

            // when
            listener.handleLineupConfirmed(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(emptyList()) }
        }
    }

    @Nested
    @DisplayName("handleGameCancelled")
    inner class HandleGameCancelled {
        @Test
        fun `should send GAME_CANCELLED notification to both teams members via batch`() {
            // given
            val event =
                GameCancelledEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                )

            val awayUser = User.createLocalUser("away@example.com", "pw", "원정멤버")
            setFieldId(awayUser, User::class.java, 11L)
            val awayPlayer = Player(name = "원정멤버", primaryPosition = Position.CATCHER)
            awayUser.player = awayPlayer

            val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2015)
            setFieldId(awayTeam, Team::class.java, 2L)
            val awayMember = TeamMember.create(awayTeam, awayUser, awayPlayer, 8, TeamMemberRole.MEMBER)

            every {
                teamMemberRepository.findByTeamIdInAndStatus(listOf(1L, 2L), TeamMemberStatus.ACTIVE)
            } returns listOf(member, awayMember)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleGameCancelled(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(2)
            assertThat(requestsSlot.captured.map { it.userId }).containsExactlyInAnyOrder(10L, 11L)
            assertThat(requestsSlot.captured.all { it.type == NotificationType.GAME_CANCELLED }).isTrue()
        }

        @Test
        fun `should skip notification when team IDs are zero`() {
            // given
            val event =
                GameCancelledEvent(
                    gameId = 100L,
                    homeTeamId = 0L,
                    awayTeamId = 0L,
                )

            // when
            listener.handleGameCancelled(event)

            // then
            verify(exactly = 0) { notificationService.sendBatchNotifications(any()) }
        }
    }

    @Nested
    @DisplayName("handleGamePostponed")
    inner class HandleGamePostponed {
        @Test
        fun `should not send notification when no active members`() {
            // given
            val newDate = LocalDateTime.of(2026, 4, 1, 14, 0)
            val event =
                GamePostponedEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    newScheduledAt = newDate,
                )

            every {
                teamMemberRepository.findByTeamIdInAndStatus(listOf(1L, 2L), TeamMemberStatus.ACTIVE)
            } returns emptyList()

            // when
            listener.handleGamePostponed(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(emptyList()) }
        }

        @Test
        fun `should send GAME_POSTPONED notification to both teams members via batch`() {
            // given
            val newDate = LocalDateTime.of(2026, 4, 1, 14, 0)
            val event =
                GamePostponedEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    newScheduledAt = newDate,
                )

            val awayUser = User.createLocalUser("away@example.com", "pw", "원정멤버")
            setFieldId(awayUser, User::class.java, 11L)
            val awayPlayer = Player(name = "원정멤버", primaryPosition = Position.CATCHER)
            awayUser.player = awayPlayer

            val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2015)
            setFieldId(awayTeam, Team::class.java, 2L)
            val awayMember = TeamMember.create(awayTeam, awayUser, awayPlayer, 8, TeamMemberRole.MEMBER)

            every {
                teamMemberRepository.findByTeamIdInAndStatus(listOf(1L, 2L), TeamMemberStatus.ACTIVE)
            } returns listOf(member, awayMember)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleGamePostponed(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(2)
            assertThat(requestsSlot.captured.map { it.userId }).containsExactlyInAnyOrder(10L, 11L)
            assertThat(requestsSlot.captured.all { it.type == NotificationType.GAME_POSTPONED }).isTrue()
            assertThat(requestsSlot.captured[0].body).contains("2026-04-01")
        }
    }

    @Nested
    @DisplayName("handleGameRescheduled")
    inner class HandleGameRescheduled {
        @Test
        fun `should not send notification when no active members`() {
            // given
            val newDate = LocalDateTime.of(2026, 5, 10, 18, 0)
            val event =
                GameRescheduledEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    newScheduledAt = newDate,
                )

            every {
                teamMemberRepository.findByTeamIdInAndStatus(listOf(1L, 2L), TeamMemberStatus.ACTIVE)
            } returns emptyList()

            // when
            listener.handleGameRescheduled(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(emptyList()) }
        }

        @Test
        fun `should send GAME_RESCHEDULED notification to both teams members via batch`() {
            // given
            val newDate = LocalDateTime.of(2026, 5, 10, 18, 0)
            val event =
                GameRescheduledEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    newScheduledAt = newDate,
                )

            val awayUser = User.createLocalUser("away@example.com", "pw", "원정멤버")
            setFieldId(awayUser, User::class.java, 11L)
            val awayPlayer = Player(name = "원정멤버", primaryPosition = Position.CATCHER)
            awayUser.player = awayPlayer

            val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2015)
            setFieldId(awayTeam, Team::class.java, 2L)
            val awayMember = TeamMember.create(awayTeam, awayUser, awayPlayer, 8, TeamMemberRole.MEMBER)

            every {
                teamMemberRepository.findByTeamIdInAndStatus(listOf(1L, 2L), TeamMemberStatus.ACTIVE)
            } returns listOf(member, awayMember)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleGameRescheduled(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(2)
            assertThat(requestsSlot.captured.map { it.userId }).containsExactlyInAnyOrder(10L, 11L)
            assertThat(requestsSlot.captured.all { it.type == NotificationType.GAME_RESCHEDULED }).isTrue()
            assertThat(requestsSlot.captured[0].body).contains("2026-05-10")
        }
    }

    @Nested
    @DisplayName("handleTeamMemberLeft")
    inner class HandleTeamMemberLeft {
        @Test
        fun `should send TEAM_MEMBER_LEFT notification to team admins via batch`() {
            // given
            val event =
                TeamMemberLeftEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            val ownerUser = User.createLocalUser("owner@example.com", "pw", "팀장")
            setFieldId(ownerUser, User::class.java, 50L)
            val ownerPlayer = Player(name = "팀장", primaryPosition = Position.STARTING_PITCHER)
            ownerUser.player = ownerPlayer
            val ownerMember = TeamMember.create(team, ownerUser, ownerPlayer, 1, TeamMemberRole.OWNER)

            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(ownerMember)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(1)
            assertThat(requestsSlot.captured[0].userId).isEqualTo(50L)
            assertThat(requestsSlot.captured[0].type).isEqualTo(NotificationType.TEAM_MEMBER_LEFT)
            assertThat(requestsSlot.captured[0].body).contains("타이거즈")
        }

        @Test
        fun `should not send notification to regular members`() {
            // given
            val event =
                TeamMemberLeftEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            // member has MEMBER role, not OWNER or MANAGER
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(member)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleTeamMemberLeft(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).isEmpty()
        }
    }

    @Nested
    @DisplayName("handleTeamMemberKicked")
    inner class HandleTeamMemberKicked {
        @Test
        fun `should send TEAM_MEMBER_KICKED notification to the kicked member`() {
            // given
            val event =
                TeamMemberKickedEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            val requestSlot = slot<SendNotificationRequest>()
            every { notificationService.sendNotification(capture(requestSlot)) } returns mockk()

            // when
            listener.handleTeamMemberKicked(event)

            // then
            verify(exactly = 1) { notificationService.sendNotification(any()) }
            assertThat(requestSlot.captured.userId).isEqualTo(10L)
            assertThat(requestSlot.captured.type).isEqualTo(NotificationType.TEAM_MEMBER_KICKED)
            assertThat(requestSlot.captured.body).contains("타이거즈")
        }
    }

    @Nested
    @DisplayName("handleElectionTied")
    inner class HandleElectionTied {
        @Test
        fun `should send ELECTION_TIED notification to team admins`() {
            // given
            val event =
                ElectionTiedEvent(
                    teamId = 1L,
                    electionId = 42L,
                    electionType = ElectionType.OWNER_ELECTION,
                    tiedCandidateCount = 3,
                    tiedVoteCount = 5L,
                )

            val ownerUser = User.createLocalUser("owner@example.com", "pw", "팀장")
            setFieldId(ownerUser, User::class.java, 50L)
            val ownerPlayer = Player(name = "팀장", primaryPosition = Position.STARTING_PITCHER)
            ownerUser.player = ownerPlayer
            val ownerMember = TeamMember.create(team, ownerUser, ownerPlayer, 1, TeamMemberRole.OWNER)

            val managerUser = User.createLocalUser("manager@example.com", "pw", "매니저")
            setFieldId(managerUser, User::class.java, 51L)
            val managerPlayer = Player(name = "매니저", primaryPosition = Position.CATCHER)
            managerUser.player = managerPlayer
            val managerMember = TeamMember.create(team, managerUser, managerPlayer, 2, TeamMemberRole.MANAGER)

            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(ownerMember, managerMember, member)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleElectionTied(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(2)
            assertThat(requestsSlot.captured.map { it.userId }).containsExactlyInAnyOrder(50L, 51L)
            assertThat(requestsSlot.captured.all { it.type == NotificationType.ELECTION_TIED }).isTrue()
            assertThat(requestsSlot.captured[0].body).contains("42").contains("3").contains("5")
        }

        @Test
        fun `should not send notification to regular members`() {
            // given
            val event =
                ElectionTiedEvent(
                    teamId = 1L,
                    electionId = 42L,
                    electionType = ElectionType.OWNER_ELECTION,
                    tiedCandidateCount = 2,
                    tiedVoteCount = 3L,
                )

            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(member)
            val requestsSlot = slot<List<SendNotificationRequest>>()
            every { notificationService.sendBatchNotifications(capture(requestsSlot)) } returns emptyList()

            // when
            listener.handleElectionTied(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).isEmpty()
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
