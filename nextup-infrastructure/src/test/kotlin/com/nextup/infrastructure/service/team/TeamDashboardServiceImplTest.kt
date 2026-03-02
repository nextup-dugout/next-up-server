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
            every { gameTeamRepository.findAllByCompetitionId(100L) } returns listOf(homeGameTeam, awayGameTeam)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(100L)
            } returns emptyList()
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

        @Test
        @DisplayName("취소된 경기(CANCELLED)가 recentResults에 포함된다")
        fun `취소된 경기가 최근 결과에 포함된다`() {
            // given
            val cancelledGame =
                makeGame(
                    id = 20L,
                    scheduledAt = LocalDateTime.now().minusDays(1),
                    status = GameStatus.CANCELLED,
                )
            val homeGameTeam = makeGameTeam(cancelledGame, team, HomeAway.HOME)
            val awayTeam =
                Team(league = league, name = "상대팀", city = "부산", foundedYear = 2021, id = 2L)
            val awayGameTeam = makeGameTeam(cancelledGame, awayTeam, HomeAway.AWAY)

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(homeGameTeam)
            every { gameRepository.findAllByIds(listOf(20L)) } returns listOf(cancelledGame)
            every { gameTeamRepository.findAllByGameIds(listOf(20L)) } returns listOf(homeGameTeam, awayGameTeam)
            every { gameTeamRepository.findAllByCompetitionId(100L) } returns listOf(homeGameTeam, awayGameTeam)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(100L)
            } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.recentResults).hasSize(1)
            assertThat(result.recentResults[0].gameId).isEqualTo(20L)
            assertThat(result.recentResults[0].status).isEqualTo(GameStatus.CANCELLED)
        }

        @Test
        @DisplayName("homeTeam/awayTeam이 없는 경기 요약은 기본값(0L, 빈문자열)을 사용한다")
        fun `gameTeams가 없을 때 game summary는 기본값을 반환한다`() {
            // given
            val futureGame =
                makeGame(
                    id = 30L,
                    scheduledAt = LocalDateTime.now().plusDays(5),
                )

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            // allGameTeams에 gameId=30 포함 (team 참여 표시)
            val dummyGameTeam = makeGameTeam(futureGame, team, HomeAway.HOME)
            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(dummyGameTeam)
            every { gameRepository.findAllByIds(listOf(30L)) } returns listOf(futureGame)
            // findAllByGameIds 결과는 빈 리스트 — homeTeam/awayTeam 모두 null
            every { gameTeamRepository.findAllByGameIds(listOf(30L)) } returns emptyList()
            every { gameTeamRepository.findAllByCompetitionId(100L) } returns listOf(dummyGameTeam)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(100L)
            } returns emptyList()
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.nextGame).isNotNull
            assertThat(result.nextGame!!.homeTeamId).isEqualTo(0L)
            assertThat(result.nextGame!!.homeTeamName).isEqualTo("")
            assertThat(result.nextGame!!.awayTeamId).isEqualTo(0L)
            assertThat(result.nextGame!!.awayTeamName).isEqualTo("")
        }

        @Test
        @DisplayName("무승부만 있을 때 winningPercentage는 0.000이다")
        fun `무승부만 있을 때 팀 통계의 winningPercentage는 0이다`() {
            // given
            val finishedGame =
                makeGame(
                    id = 40L,
                    scheduledAt = LocalDateTime.now().minusDays(2),
                    status = GameStatus.FINISHED,
                )
            val homeGameTeam =
                makeGameTeam(finishedGame, team, HomeAway.HOME, score = 3, result = GameResult.DRAW)
            val awayTeam =
                Team(league = league, name = "상대팀", city = "부산", foundedYear = 2021, id = 2L)
            val awayGameTeam =
                makeGameTeam(finishedGame, awayTeam, HomeAway.AWAY, score = 3, result = GameResult.DRAW)

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(homeGameTeam)
            every { gameRepository.findAllByIds(listOf(40L)) } returns listOf(finishedGame)
            every { gameTeamRepository.findAllByGameIds(listOf(40L)) } returns listOf(homeGameTeam, awayGameTeam)
            every { gameTeamRepository.findAllByCompetitionId(100L) } returns listOf(homeGameTeam, awayGameTeam)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(100L)
            } returns listOf(homeGameTeam, awayGameTeam)
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.teamStats).isNotNull
            assertThat(result.teamStats!!.draws).isEqualTo(1)
            assertThat(result.teamStats!!.wins).isEqualTo(0)
            assertThat(result.teamStats!!.losses).isEqualTo(0)
            // wins + losses == 0 분기 → winningPercentage == 0.000
            assertThat(result.teamStats!!.winningPercentage.toPlainString()).isEqualTo("0.000")
        }

        @Test
        @DisplayName("팀이 리더가 아닐 때 gamesBehind가 올바르게 계산된다")
        fun `순위에서 리더가 아닌 팀의 gamesBehind가 계산된다`() {
            // given
            val leaderTeam =
                Team(league = league, name = "1위팀", city = "서울", foundedYear = 2019, id = 10L)
            val game1 =
                makeGame(
                    id = 50L,
                    scheduledAt = LocalDateTime.now().minusDays(5),
                    status = GameStatus.FINISHED,
                )
            val game2 =
                makeGame(
                    id = 51L,
                    scheduledAt = LocalDateTime.now().minusDays(4),
                    status = GameStatus.FINISHED,
                )

            // leaderTeam: 2승 0패 / team(id=1): 0승 2패
            val gt1Home =
                makeGameTeam(game1, leaderTeam, HomeAway.HOME, score = 5, result = GameResult.WIN)
            val gt1Away =
                makeGameTeam(game1, team, HomeAway.AWAY, score = 1, result = GameResult.LOSS)
            val gt2Home =
                makeGameTeam(game2, leaderTeam, HomeAway.HOME, score = 4, result = GameResult.WIN)
            val gt2Away =
                makeGameTeam(game2, team, HomeAway.AWAY, score = 2, result = GameResult.LOSS)

            // team(id=1)이 참가한 gameTeams
            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(gt1Away, gt2Away)
            every { gameRepository.findAllByIds(listOf(50L, 51L)) } returns listOf(game1, game2)
            every {
                gameTeamRepository.findAllByGameIds(listOf(50L, 51L))
            } returns listOf(gt1Home, gt1Away, gt2Home, gt2Away)
            // 대회 전체 gameTeams
            every { gameTeamRepository.findAllByCompetitionId(100L) } returns
                listOf(gt1Home, gt1Away, gt2Home, gt2Away)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(100L)
            } returns listOf(gt1Home, gt1Away, gt2Home, gt2Away)
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then
            assertThat(result.standing).isNotNull
            // leaderTeam: 2W 0L, team: 0W 2L → gamesBehind = (2-0 + 2-0)/2 = 2.0
            assertThat(result.standing!!.gamesBehind).isEqualByComparingTo("2.0")
            assertThat(result.standing!!.rank).isEqualTo(2)
        }

        @Test
        @DisplayName("팀이 대회 순위 목록에 없을 때 standing은 null이다")
        fun `대회 순위에서 팀을 찾을 수 없으면 standing은 null이다`() {
            // given
            val otherTeam1 =
                Team(league = league, name = "다른팀A", city = "인천", foundedYear = 2018, id = 20L)
            val otherTeam2 =
                Team(league = league, name = "다른팀B", city = "수원", foundedYear = 2019, id = 21L)
            val game =
                makeGame(
                    id = 60L,
                    scheduledAt = LocalDateTime.now().minusDays(1),
                    status = GameStatus.FINISHED,
                )

            // team(id=1)은 allGameTeams에는 있지만 competition의 전체 gameTeams에는 없는 상황
            val teamGt = makeGameTeam(game, team, HomeAway.HOME, score = 3, result = GameResult.WIN)
            val gt1 =
                makeGameTeam(game, otherTeam1, HomeAway.HOME, score = 5, result = GameResult.WIN)
            val gt2 =
                makeGameTeam(game, otherTeam2, HomeAway.AWAY, score = 2, result = GameResult.LOSS)

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(teamGt)
            every { gameRepository.findAllByIds(listOf(60L)) } returns listOf(game)
            every { gameTeamRepository.findAllByGameIds(listOf(60L)) } returns listOf(gt1, gt2)
            // competitionId 100L의 전체 목록에는 team(id=1)이 없음
            every { gameTeamRepository.findAllByCompetitionId(100L) } returns listOf(gt1, gt2)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(100L)
            } returns listOf(gt1, gt2)
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then — teamIndex < 0 분기 → standing == null
            assertThat(result.standing).isNull()
        }

        @Test
        @DisplayName("두 개의 대회에 참여 중일 때 경기 수가 많은 대회를 기준으로 순위를 반환한다")
        fun `여러 대회 참여 시 경기가 많은 대회 기준으로 standing을 반환한다`() {
            // given
            val competition2 =
                Competition(
                    league = league,
                    name = "여름리그",
                    type = CompetitionType.LEAGUE,
                    year = 2026,
                    startDate = LocalDate.of(2026, 6, 1),
                    id = 200L,
                )

            // competition(id=100): 1경기, competition2(id=200): 2경기
            val game1 =
                makeGame(
                    id = 70L,
                    scheduledAt = LocalDateTime.now().minusDays(10),
                    status = GameStatus.FINISHED,
                )
            val game2 =
                makeGame(
                    id = 71L,
                    scheduledAt = LocalDateTime.now().minusDays(5),
                    status = GameStatus.FINISHED,
                )
            val game3 =
                makeGame(
                    id = 72L,
                    scheduledAt = LocalDateTime.now().minusDays(3),
                    status = GameStatus.FINISHED,
                )
            // game2, game3은 competition2 소속으로 field override
            val field = game2::class.java.getDeclaredField("competition")
            field.isAccessible = true
            field.set(game2, competition2)
            field.set(game3, competition2)

            val awayTeam =
                Team(league = league, name = "상대팀", city = "부산", foundedYear = 2021, id = 2L)

            val gt1Home = makeGameTeam(game1, team, HomeAway.HOME, score = 3, result = GameResult.WIN)
            val gt1Away = makeGameTeam(game1, awayTeam, HomeAway.AWAY, score = 1, result = GameResult.LOSS)
            val gt2Home = makeGameTeam(game2, team, HomeAway.HOME, score = 4, result = GameResult.WIN)
            val gt2Away = makeGameTeam(game2, awayTeam, HomeAway.AWAY, score = 2, result = GameResult.LOSS)
            val gt3Home = makeGameTeam(game3, team, HomeAway.HOME, score = 5, result = GameResult.WIN)
            val gt3Away = makeGameTeam(game3, awayTeam, HomeAway.AWAY, score = 0, result = GameResult.LOSS)

            every { teamRepository.findByIdWithLeague(1L) } returns team
            every { teamMembershipService.getTeamMemberCount(1L) } returns 10
            // team이 참여한 gameTeams: comp100 1경기, comp200 2경기
            every {
                gameTeamRepository.findAllByTeamId(1L)
            } returns listOf(gt1Home, gt2Home, gt3Home)
            every {
                gameRepository.findAllByIds(listOf(70L, 71L, 72L))
            } returns listOf(game1, game2, game3)
            every {
                gameTeamRepository.findAllByGameIds(listOf(70L, 71L, 72L))
            } returns listOf(gt1Home, gt1Away, gt2Home, gt2Away, gt3Home, gt3Away)
            // primaryCompetition은 경기 수가 많은 200L
            every { gameTeamRepository.findAllByCompetitionId(200L) } returns
                listOf(gt2Home, gt2Away, gt3Home, gt3Away)
            every {
                gameTeamRepository.findAllByCompetitionIdWithDecidedResult(200L)
            } returns listOf(gt2Home, gt2Away, gt3Home, gt3Away)
            every { attendancePollRepository.findByTeamId(1L, PollStatus.OPEN) } returns emptyList()

            // when
            val result = service.getTeamDashboard(1L)

            // then — competition2(id=200, "여름리그") 기준
            assertThat(result.standing).isNotNull
            assertThat(result.standing!!.competitionId).isEqualTo(200L)
            assertThat(result.standing!!.competitionName).isEqualTo("여름리그")
            assertThat(result.standing!!.wins).isEqualTo(2)
        }
    }
}
