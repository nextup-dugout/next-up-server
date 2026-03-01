package com.nextup.infrastructure.service.team

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.team.TeamMembershipService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("TeamDashboardServiceImpl")
class TeamDashboardServiceImplTest {
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var teamMembershipService: TeamMembershipService
    private lateinit var attendancePollRepository: AttendancePollRepositoryPort
    private lateinit var service: TeamDashboardServiceImpl

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var team: Team
    private lateinit var competition: Competition

    @BeforeEach
    fun setUp() {
        teamRepository = mockk()
        gameRepository = mockk()
        gameTeamRepository = mockk()
        teamMembershipService = mockk()
        attendancePollRepository = mockk()

        service =
            TeamDashboardServiceImpl(
                teamRepository,
                gameRepository,
                gameTeamRepository,
                teamMembershipService,
                attendancePollRepository,
            )

        association = Association(name = "테스트 협회", id = 1L)
        league = League(association = association, name = "서울 리그", foundedYear = 2020, id = 1L)
        team = Team(league = league, name = "테스트팀", city = "서울", foundedYear = 2020, id = 1L)
        competition =
            Competition(
                league = league,
                name = "봄리그",
                type = CompetitionType.LEAGUE,
                year = 2026,
                startDate = LocalDate.of(2026, 3, 1),
                id = 100L,
            )
    }

    private fun makeGame(
        id: Long,
        scheduledAt: LocalDateTime,
        status: GameStatus = GameStatus.SCHEDULED,
    ): Game =
        Game(
            competition = competition,
            scheduledAt = scheduledAt,
            id = id,
        ).also {
            // Use reflection to set status for test purposes or rely on status from constructor default
            if (status != GameStatus.SCHEDULED) {
                // Status is set via business methods or directly for testing purposes
                val field = Game::class.java.getDeclaredField("status")
                field.isAccessible = true
                field.set(it, status)
            }
        }

    private fun makeGameTeam(
        game: Game,
        team: Team,
        homeAway: HomeAway,
        score: Int = 0,
        result: GameResult = GameResult.UNDECIDED,
    ): GameTeam {
        val gt = GameTeam(game = game, team = team, homeAway = homeAway)
        if (score > 0) {
            gt.updateScore(totalScore = score, totalHits = 0, totalErrors = 0)
        }
        if (result != GameResult.UNDECIDED) {
            gt.updateResult(result)
        }
        return gt
    }

    @Nested
    @DisplayName("getTeamDashboard")
    inner class GetTeamDashboard {
        @Test
        fun `팀 대시보드를 성공적으로 반환한다`() {
            // given
            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 15
            every { gameTeamRepository.findAllByTeamId(1L) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.team.teamId).isEqualTo(1L)
            assertThat(result.team.name).isEqualTo("테스트팀")
            assertThat(result.team.city).isEqualTo("서울")
            assertThat(result.memberCount).isEqualTo(15)
            assertThat(result.nextGame).isNull()
            assertThat(result.recentResults).isEmpty()
            assertThat(result.standing).isNull()
            assertThat(result.activePoll).isNull()
            assertThat(result.teamStats).isNull()
        }

        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException을 던진다`() {
            // given
            every { teamRepository.findByIdWithLeague(999L) } returns null

            // when & then
            assertThrows<TeamNotFoundException> {
                service.getTeamDashboard(999L)
            }
        }

        @Test
        fun `다음 예정 경기를 반환한다`() {
            // given
            val futureGame = makeGame(id = 10L, scheduledAt = LocalDateTime.now().plusDays(3))
            val homeGameTeam = makeGameTeam(futureGame, team, HomeAway.HOME)
            val awayTeam = Team(league = league, name = "상대팀", city = "부산", foundedYear = 2021, id = 2L)
            val awayGameTeam = makeGameTeam(futureGame, awayTeam, HomeAway.AWAY)

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(homeGameTeam)
            every { gameRepository.findAllByIds(listOf(10L)) } returns listOf(futureGame)
            every { gameTeamRepository.findAllByGameIds(listOf(10L)) } returns listOf(homeGameTeam, awayGameTeam)
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.nextGame).isNotNull
            assertThat(result.nextGame!!.gameId).isEqualTo(10L)
            assertThat(result.nextGame!!.homeTeamId).isEqualTo(1L)
            assertThat(result.nextGame!!.homeTeamName).isEqualTo("테스트팀")
            assertThat(result.nextGame!!.status).isEqualTo(GameStatus.SCHEDULED)
        }

        @Test
        fun `최근 완료된 경기 결과를 반환한다`() {
            // given
            val finishedGame =
                makeGame(
                    id = 5L,
                    scheduledAt = LocalDateTime.now().minusDays(3),
                    status = GameStatus.FINISHED,
                )
            val homeGameTeam = makeGameTeam(finishedGame, team, HomeAway.HOME, score = 5, result = GameResult.WIN)
            val awayTeam = Team(league = league, name = "상대팀", city = "부산", foundedYear = 2021, id = 2L)
            val awayGameTeam = makeGameTeam(finishedGame, awayTeam, HomeAway.AWAY, score = 3, result = GameResult.LOSS)

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(homeGameTeam)
            every { gameRepository.findAllByIds(listOf(5L)) } returns listOf(finishedGame)
            every { gameTeamRepository.findAllByGameIds(listOf(5L)) } returns listOf(homeGameTeam, awayGameTeam)
            every { gameTeamRepository.findAllByCompetitionId(100L) } returns listOf(homeGameTeam, awayGameTeam)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(100L)
            } returns listOf(homeGameTeam, awayGameTeam)
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.recentResults).hasSize(1)
            assertThat(result.recentResults[0].gameId).isEqualTo(5L)
            assertThat(result.recentResults[0].homeScore).isEqualTo(5)
            assertThat(result.recentResults[0].awayScore).isEqualTo(3)
        }

        @Test
        fun `진행 중인 출석 투표를 activePoll로 반환한다`() {
            // given
            val poll =
                AttendancePoll.create(
                    team = team,
                    title = "이번 주 경기 참석 여부",
                    eventDate = LocalDateTime.now().plusDays(5),
                    deadline = LocalDateTime.now().plusDays(3),
                )

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns listOf(poll)

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.activePoll).isNotNull
            assertThat(result.activePoll!!.title).isEqualTo("이번 주 경기 참석 여부")
            assertThat(result.activePoll!!.status).isEqualTo(PollStatus.OPEN)
        }

        @Test
        fun `진행 중인 투표가 없으면 activePoll은 null이다`() {
            // given
            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.activePoll).isNull()
        }

        @Test
        fun `경기가 있을 때 팀 통계 요약을 반환한다`() {
            // given
            val finishedGame =
                makeGame(
                    id = 5L,
                    scheduledAt = LocalDateTime.now().minusDays(3),
                    status = GameStatus.FINISHED,
                )
            val homeGameTeam = makeGameTeam(finishedGame, team, HomeAway.HOME, score = 5, result = GameResult.WIN)
            val awayTeam = Team(league = league, name = "상대팀", city = "부산", foundedYear = 2021, id = 2L)
            val awayGameTeam = makeGameTeam(finishedGame, awayTeam, HomeAway.AWAY, score = 3, result = GameResult.LOSS)

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(homeGameTeam)
            every { gameRepository.findAllByIds(listOf(5L)) } returns listOf(finishedGame)
            every { gameTeamRepository.findAllByGameIds(listOf(5L)) } returns listOf(homeGameTeam, awayGameTeam)
            every { gameTeamRepository.findAllByCompetitionId(100L) } returns listOf(homeGameTeam, awayGameTeam)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(100L)
            } returns listOf(homeGameTeam, awayGameTeam)
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.teamStats).isNotNull
            assertThat(result.teamStats!!.gamesPlayed).isEqualTo(1)
            assertThat(result.teamStats!!.wins).isEqualTo(1)
            assertThat(result.teamStats!!.losses).isEqualTo(0)
        }
    }
}
