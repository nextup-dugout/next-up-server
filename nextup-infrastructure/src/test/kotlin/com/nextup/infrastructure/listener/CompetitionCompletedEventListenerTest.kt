package com.nextup.infrastructure.listener

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.CompetitionCompletedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.GameTeamRepositoryPort
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
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("CompetitionCompletedEventListener 테스트")
class CompetitionCompletedEventListenerTest {
    private lateinit var notificationService: NotificationService
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var listener: CompetitionCompletedEventListener

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var homeTeam: Team
    private lateinit var awayTeam: Team
    private lateinit var competition: Competition

    @BeforeEach
    fun setUp() {
        notificationService = mockk(relaxed = true)
        teamMemberRepository = mockk()
        gameTeamRepository = mockk()

        listener =
            CompetitionCompletedEventListener(
                notificationService = notificationService,
                teamMemberRepository = teamMemberRepository,
                gameTeamRepository = gameTeamRepository,
            )

        association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2015)
        setFieldId(homeTeam, Team::class.java, 1L)
        awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2015)
        setFieldId(awayTeam, Team::class.java, 2L)
        competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
            )
        setFieldId(competition, Competition::class.java, 10L)
    }

    @Nested
    @DisplayName("handleCompetitionCompleted - 대회 완료 이벤트 처리")
    inner class HandleCompetitionCompleted {
        @Test
        fun `대회 참가 팀 멤버에게 대회 완료 알림을 배치 발송한다`() {
            // given
            val event =
                CompetitionCompletedEvent(
                    competitionId = 10L,
                    competitionName = "2025 춘계대회",
                    leagueId = 1L,
                )

            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                    status = GameStatus.FINISHED,
                    totalInnings = 9,
                    id = 100L,
                )

            val homeGameTeam = GameTeam(game = game, team = homeTeam, homeAway = HomeAway.HOME)
            val awayGameTeam = GameTeam(game = game, team = awayTeam, homeAway = HomeAway.AWAY)

            val homeUser = User.createLocalUser("home@example.com", "pw", "홈멤버")
            setFieldId(homeUser, User::class.java, 30L)
            val homePlayer = Player(name = "홈멤버", primaryPosition = Position.SHORTSTOP)
            setFieldId(homePlayer, Player::class.java, 130L)
            homeUser.player = homePlayer
            val homeMember =
                TeamMember.create(homeTeam, homeUser, homePlayer, 1, TeamMemberRole.MEMBER)

            val awayUser = User.createLocalUser("away@example.com", "pw", "원정멤버")
            setFieldId(awayUser, User::class.java, 31L)
            val awayPlayer = Player(name = "원정멤버", primaryPosition = Position.CATCHER)
            setFieldId(awayPlayer, Player::class.java, 131L)
            awayUser.player = awayPlayer
            val awayMember =
                TeamMember.create(awayTeam, awayUser, awayPlayer, 2, TeamMemberRole.MEMBER)

            every {
                gameTeamRepository.findAllByCompetitionId(10L)
            } returns listOf(homeGameTeam, awayGameTeam)
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(homeMember)
            every {
                teamMemberRepository.findByTeamIdAndStatus(2L, TeamMemberStatus.ACTIVE)
            } returns listOf(awayMember)

            val requestsSlot = slot<List<SendNotificationRequest>>()
            every {
                notificationService.sendBatchNotifications(capture(requestsSlot))
            } returns emptyList()

            // when
            listener.handleCompetitionCompleted(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(2)
            assertThat(requestsSlot.captured.map { it.userId })
                .containsExactlyInAnyOrder(30L, 31L)
            assertThat(
                requestsSlot.captured.all {
                    it.type == NotificationType.COMPETITION_COMPLETED
                },
            ).isTrue()
            assertThat(requestsSlot.captured[0].body).contains("2025 춘계대회")
        }

        @Test
        fun `대회에 참가한 팀이 없으면 알림을 발송하지 않는다`() {
            // given
            val event =
                CompetitionCompletedEvent(
                    competitionId = 10L,
                    competitionName = "2025 춘계대회",
                    leagueId = 1L,
                )

            every { gameTeamRepository.findAllByCompetitionId(10L) } returns emptyList()

            // when
            listener.handleCompetitionCompleted(event)

            // then
            verify(exactly = 0) { notificationService.sendBatchNotifications(any()) }
        }

        @Test
        fun `활성 멤버가 없는 팀은 알림 대상에서 제외된다`() {
            // given
            val event =
                CompetitionCompletedEvent(
                    competitionId = 10L,
                    competitionName = "2025 춘계대회",
                    leagueId = 1L,
                )

            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                    status = GameStatus.FINISHED,
                    totalInnings = 9,
                    id = 100L,
                )

            val homeGameTeam = GameTeam(game = game, team = homeTeam, homeAway = HomeAway.HOME)
            val awayGameTeam = GameTeam(game = game, team = awayTeam, homeAway = HomeAway.AWAY)

            every {
                gameTeamRepository.findAllByCompetitionId(10L)
            } returns listOf(homeGameTeam, awayGameTeam)
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns emptyList()
            every {
                teamMemberRepository.findByTeamIdAndStatus(2L, TeamMemberStatus.ACTIVE)
            } returns emptyList()

            // when
            listener.handleCompetitionCompleted(event)

            // then
            verify(exactly = 0) { notificationService.sendBatchNotifications(any()) }
        }

        @Test
        fun `동일 팀이 여러 경기에 참가해도 알림은 중복 발송되지 않는다`() {
            // given
            val event =
                CompetitionCompletedEvent(
                    competitionId = 10L,
                    competitionName = "2025 춘계대회",
                    leagueId = 1L,
                )

            val game1 =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                    status = GameStatus.FINISHED,
                    totalInnings = 9,
                    id = 100L,
                )
            val game2 =
                Game.createForTest(
                    competition = competition,
                    homeTeam = awayTeam,
                    awayTeam = homeTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 17, 14, 0),
                    status = GameStatus.FINISHED,
                    totalInnings = 9,
                    id = 101L,
                )

            val gameTeam1Home =
                GameTeam(game = game1, team = homeTeam, homeAway = HomeAway.HOME)
            val gameTeam1Away =
                GameTeam(game = game1, team = awayTeam, homeAway = HomeAway.AWAY)
            val gameTeam2Home =
                GameTeam(game = game2, team = awayTeam, homeAway = HomeAway.HOME)
            val gameTeam2Away =
                GameTeam(game = game2, team = homeTeam, homeAway = HomeAway.AWAY)

            val homeUser = User.createLocalUser("home@example.com", "pw", "홈멤버")
            setFieldId(homeUser, User::class.java, 30L)
            val homePlayer = Player(name = "홈멤버", primaryPosition = Position.SHORTSTOP)
            setFieldId(homePlayer, Player::class.java, 130L)
            homeUser.player = homePlayer
            val homeMember =
                TeamMember.create(homeTeam, homeUser, homePlayer, 1, TeamMemberRole.MEMBER)

            every {
                gameTeamRepository.findAllByCompetitionId(10L)
            } returns listOf(gameTeam1Home, gameTeam1Away, gameTeam2Home, gameTeam2Away)
            every {
                teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)
            } returns listOf(homeMember)
            every {
                teamMemberRepository.findByTeamIdAndStatus(2L, TeamMemberStatus.ACTIVE)
            } returns emptyList()

            val requestsSlot = slot<List<SendNotificationRequest>>()
            every {
                notificationService.sendBatchNotifications(capture(requestsSlot))
            } returns emptyList()

            // when
            listener.handleCompetitionCompleted(event)

            // then
            verify(exactly = 1) { notificationService.sendBatchNotifications(any()) }
            assertThat(requestsSlot.captured).hasSize(1)
            assertThat(requestsSlot.captured[0].userId).isEqualTo(30L)
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
