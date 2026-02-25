package com.nextup.infrastructure.service.standings

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidInputException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.service.standings.dto.SimulatedGameResult
import com.nextup.core.service.standings.dto.SimulationRequest
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

@DisplayName("StandingsSimulationServiceImpl")
class StandingsSimulationServiceImplTest {
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var service: StandingsSimulationServiceImpl

    private lateinit var competition: Competition
    private lateinit var league: League
    private lateinit var association: Association

    @BeforeEach
    fun setUp() {
        competitionRepository = mockk()
        gameRepository = mockk()
        gameTeamRepository = mockk()
        service =
            StandingsSimulationServiceImpl(
                competitionRepository,
                gameRepository,
                gameTeamRepository,
            )

        association = createAssociation(1L, "서울시야구협회")
        league = createLeague(1L, "1부 리그", association)
        competition = createCompetition(1L, league, "2025 춘계대회", 2025, 1)
    }

    // =========================================================================
    // calculateMagicNumbers
    // =========================================================================

    @Nested
    @DisplayName("calculateMagicNumbers")
    inner class CalculateMagicNumbers {
        @Test
        fun `대회가 존재하지 않으면 CompetitionNotFoundException 발생`() {
            every { competitionRepository.findByIdOrNull(999L) } returns null

            assertThrows<CompetitionNotFoundException> {
                service.calculateMagicNumbers(999L)
            }
        }

        @Test
        fun `경기 기록이 없으면 빈 목록 반환`() {
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns emptyList()
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns emptyList()

            val result = service.calculateMagicNumbers(1L)

            assertThat(result).isEmpty()
        }

        @Test
        fun `선두팀의 매직넘버는 0이고 isClinched true`() {
            // Tigers: 5승 0패, 남은경기 5
            // Lions:  3승 2패, 남은경기 5
            // 선두(Tigers) 매직넘버 = 5 + 1 - (5 - 5) = 6 → 0으로 클램핑 아님
            // 실제: rank=1인 Tigers의 타겟 매직넘버 계산 기준은 자기 자신과의 비교 → rawMN = leader.remainingGames+1-(leader.wins-stats.wins)=5+1-0=6
            // => isClinched = false (6>0)
            // Lions: rawMN = 5+1-(5-3) = 4 => isClinched = false

            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val games = (1..5).map { createGame(it.toLong(), competition) }
            val remainingGames = (6..10).map { createGame(it.toLong(), competition) }

            val decidedGameTeams = mutableListOf<GameTeam>()
            val allGameTeams = mutableListOf<GameTeam>()

            // Tigers 5승, Lions 3승 2패
            games.forEachIndexed { i, game ->
                val tigersResult = GameResult.WIN
                val lionsResult = if (i < 3) GameResult.WIN else GameResult.LOSS
                val tScore = if (tigersResult == GameResult.WIN) 5 else 3
                val lScore = if (lionsResult == GameResult.WIN) 5 else 3

                val gt = createGameTeam((i * 2 + 1).toLong(), game, teamA, HomeAway.HOME, tScore, tigersResult)
                val gl = createGameTeam((i * 2 + 2).toLong(), game, teamB, HomeAway.AWAY, lScore, lionsResult)
                decidedGameTeams += listOf(gt, gl)
                allGameTeams += listOf(gt, gl)
            }

            // 남은 경기 추가 (UNDECIDED)
            remainingGames.forEachIndexed { i, game ->
                val gt = createGameTeam((100 + i * 2).toLong(), game, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
                val gl = createGameTeam((101 + i * 2).toLong(), game, teamB, HomeAway.AWAY, 0, GameResult.UNDECIDED)
                allGameTeams += listOf(gt, gl)
            }

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            val result = service.calculateMagicNumbers(1L)

            assertThat(result).hasSize(2)
            val tigersResult = result.find { it.teamId == 1L }!!
            val lionsResult = result.find { it.teamId == 2L }!!

            // Tigers(1위): magicNumber = 5+1-(5-5) = 6
            assertThat(tigersResult.magicNumber).isEqualTo(6)
            assertThat(tigersResult.isClinched).isFalse()
            assertThat(tigersResult.isEliminated).isFalse()

            // Lions(2위): magicNumber = 5+1-(5-3) = 4
            assertThat(lionsResult.magicNumber).isEqualTo(4)
            assertThat(lionsResult.isClinched).isFalse()
            assertThat(lionsResult.isEliminated).isFalse()
        }

        @Test
        fun `남은경기가 없고 선두보다 승수 적으면 isEliminated true`() {
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)
            val game3 = createGame(3L, competition)

            // Tigers: 3승 0패, 남은경기 0
            // Lions: 0승 3패, 남은경기 0
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)
            val gt2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 4, GameResult.WIN)
            val gl2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 1, GameResult.LOSS)
            val gt3 = createGameTeam(5L, game3, teamA, HomeAway.HOME, 6, GameResult.WIN)
            val gl3 = createGameTeam(6L, game3, teamB, HomeAway.AWAY, 3, GameResult.LOSS)

            val allGameTeams = listOf(gt1, gl1, gt2, gl2, gt3, gl3)
            val decidedGameTeams = listOf(gt1, gl1, gt2, gl2, gt3, gl3)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            val result = service.calculateMagicNumbers(1L)

            val lionsResult = result.find { it.teamId == 2L }!!
            // Lions: maxPossibleWins = 0+0=0 < 3(Tigers wins), remainingGames=0 → isEliminated=true
            assertThat(lionsResult.isEliminated).isTrue()
        }

        @Test
        fun `매직넘버가 음수인 경우 0으로 클램핑`() {
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)

            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            val allGameTeams = listOf(gt1, gl1)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            val result = service.calculateMagicNumbers(1L)

            result.forEach { mn ->
                assertThat(mn.magicNumber).isGreaterThanOrEqualTo(0)
            }
        }
    }

    // =========================================================================
    // simulateStandings
    // =========================================================================

    @Nested
    @DisplayName("simulateStandings")
    inner class SimulateStandings {
        @Test
        fun `대회가 존재하지 않으면 CompetitionNotFoundException 발생`() {
            every { competitionRepository.findByIdOrNull(999L) } returns null

            assertThrows<CompetitionNotFoundException> {
                service.simulateStandings(999L, SimulationRequest(emptyList()))
            }
        }

        @Test
        fun `음수 점수가 있으면 InvalidInputException 발생`() {
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns emptyList()
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns emptyList()

            val request =
                SimulationRequest(
                    listOf(SimulatedGameResult(gameId = 1L, homeScore = -1, awayScore = 3)),
                )

            assertThrows<InvalidInputException> {
                service.simulateStandings(1L, request)
            }
        }

        @Test
        fun `가상 결과를 적용하면 순위가 변동된다`() {
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            // 현재: Tigers 1승, Lions 0승
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            // 예정 경기 (game2는 UNDECIDED)
            val gt2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val gl2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 0, GameResult.UNDECIDED)

            val allGameTeams = listOf(gt1, gl1, gt2, gl2)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // Lions가 game2에서 이기는 시뮬레이션
            val request =
                SimulationRequest(
                    listOf(SimulatedGameResult(gameId = 2L, homeScore = 1, awayScore = 8)),
                )

            val result = service.simulateStandings(1L, request)

            // Tigers: 1승 1패, Lions: 1승 1패 → 득실점차로 순위 결정
            assertThat(result.standings).hasSize(2)
            // 순위 변동이 있어야 함
            assertThat(result.changes).isNotEmpty()
        }

        @Test
        fun `시뮬레이션 결과가 이미 확정된 경기에는 적용되지 않는다`() {
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)

            // game1은 이미 결과가 확정됨 (WIN/LOSS)
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            val allGameTeams = listOf(gt1, gl1)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // 이미 확정된 경기에 다른 결과를 시뮬레이션
            val request =
                SimulationRequest(
                    listOf(SimulatedGameResult(gameId = 1L, homeScore = 0, awayScore = 10)),
                )

            val result = service.simulateStandings(1L, request)

            // 확정된 경기는 변경되지 않으므로 Tigers가 여전히 1위
            assertThat(result.standings[0].teamId).isEqualTo(1L)
            assertThat(result.standings[0].wins).isEqualTo(1)
            assertThat(result.changes).isEmpty()
        }

        @Test
        fun `시뮬레이션 결과가 없으면 현재 순위와 동일하고 변동 없음`() {
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)

            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            val allGameTeams = listOf(gt1, gl1)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            val result = service.simulateStandings(1L, SimulationRequest(emptyList()))

            assertThat(result.standings).hasSize(2)
            assertThat(result.changes).isEmpty()
            assertThat(result.standings[0].teamId).isEqualTo(1L) // Tigers 1위
        }

        @Test
        fun `무승부 시뮬레이션도 올바르게 처리한다`() {
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)
            val gt2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val gl2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 0, GameResult.UNDECIDED)

            val allGameTeams = listOf(gt1, gl1, gt2, gl2)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // 무승부 시뮬레이션 (동점)
            val request =
                SimulationRequest(
                    listOf(SimulatedGameResult(gameId = 2L, homeScore = 3, awayScore = 3)),
                )

            val result = service.simulateStandings(1L, request)

            // Tigers: 1승 0패 1무, Lions: 0승 1패 1무
            val tigers = result.standings.find { it.teamId == 1L }!!
            val lions = result.standings.find { it.teamId == 2L }!!
            assertThat(tigers.draws).isEqualTo(1)
            assertThat(lions.draws).isEqualTo(1)
        }
    }

    // =========================================================================
    // calculatePlayoffScenarios
    // =========================================================================

    @Nested
    @DisplayName("calculatePlayoffScenarios")
    inner class CalculatePlayoffScenarios {
        @Test
        fun `대회가 존재하지 않으면 CompetitionNotFoundException 발생`() {
            every { competitionRepository.findByIdOrNull(999L) } returns null

            assertThrows<CompetitionNotFoundException> {
                service.calculatePlayoffScenarios(999L, 1L, 4)
            }
        }

        @Test
        fun `playoffTeams가 0이하이면 InvalidInputException 발생`() {
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns emptyList()
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns emptyList()

            assertThrows<InvalidInputException> {
                service.calculatePlayoffScenarios(1L, 1L, 0)
            }
        }

        @Test
        fun `해당 팀이 대회에 없으면 InvalidInputException 발생`() {
            val teamA = createTeam(1L, league, "Tigers")
            val game1 = createGame(1L, competition)
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns listOf(gt1)
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns listOf(gt1)

            assertThrows<InvalidInputException> {
                // teamId=999 는 대회에 없는 팀
                service.calculatePlayoffScenarios(1L, 999L, 4)
            }
        }

        @Test
        fun `남은 경기가 없을 때 확정적인 결과를 반환한다`() {
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")
            val teamC = createTeam(3L, league, "Bears")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            // 모든 경기 완료: Tigers 2승, Lions 1승 1패, Bears 0승 2패
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)
            val gt2 = createGameTeam(3L, game2, teamB, HomeAway.HOME, 4, GameResult.WIN)
            val gl2 = createGameTeam(4L, game2, teamC, HomeAway.AWAY, 1, GameResult.LOSS)

            // game3: Tigers vs Bears - Tigers WIN
            val game3 = createGame(3L, competition)
            val gt3 = createGameTeam(5L, game3, teamA, HomeAway.HOME, 3, GameResult.WIN)
            val gl3 = createGameTeam(6L, game3, teamC, HomeAway.AWAY, 0, GameResult.LOSS)

            val allGameTeams = listOf(gt1, gl1, gt2, gl2, gt3, gl3)
            val decidedGameTeams = listOf(gt1, gl1, gt2, gl2, gt3, gl3)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // Tigers의 플레이오프 진출 시나리오 (상위 2팀)
            val result = service.calculatePlayoffScenarios(1L, 1L, 2)

            assertThat(result.totalScenarios).isGreaterThan(0)
            // 모든 경기 완료 → Tigers는 1위 확정 → 100% 진출
            assertThat(result.probability).isEqualTo(1.0)
        }

        @Test
        fun `플레이오프 진출이 불가능한 팀의 확률은 0이다`() {
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")
            val teamC = createTeam(3L, league, "Bears")

            // 3팀 리그에서 상위 1팀만 플레이오프 진출
            // Lions와 Bears는 각각 경기가 남아있고, Tigers는 꼴찌 확정
            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            // Tigers: 0승 2패, Lions/Bears: 1승씩
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 0, GameResult.LOSS)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 5, GameResult.WIN)
            val gt2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 0, GameResult.LOSS)
            val gl2 = createGameTeam(4L, game2, teamC, HomeAway.AWAY, 3, GameResult.WIN)

            // 남은 경기 없음 - Tigers는 이미 꼴찌 확정
            val allGameTeams = listOf(gt1, gl1, gt2, gl2)
            val decidedGameTeams = listOf(gt1, gl1, gt2, gl2)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // Tigers의 1위 진출 시나리오
            val result = service.calculatePlayoffScenarios(1L, 1L, 1)

            assertThat(result.probability).isEqualTo(0.0)
        }
    }

    // =========================================================================
    // Additional coverage tests
    // =========================================================================

    @Nested
    @DisplayName("calculateMagicNumbers - 추가 시나리오")
    inner class CalculateMagicNumbersExtra {
        @Test
        fun `선두팀 남은경기 없고 경기 뛴 경우 isClinched true`() {
            // given: Tigers 3승 0패, 남은 경기 없음 → leader.remainingGames == 0 && gamesPlayed > 0 → isClinched true
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)
            val game3 = createGame(3L, competition)

            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)
            val gt2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 4, GameResult.WIN)
            val gl2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 1, GameResult.LOSS)
            val gt3 = createGameTeam(5L, game3, teamA, HomeAway.HOME, 6, GameResult.WIN)
            val gl3 = createGameTeam(6L, game3, teamB, HomeAway.AWAY, 3, GameResult.LOSS)

            val allGameTeams = listOf(gt1, gl1, gt2, gl2, gt3, gl3)
            val decidedGameTeams = listOf(gt1, gl1, gt2, gl2, gt3, gl3)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = service.calculateMagicNumbers(1L)

            // then: Tigers(1위) - 남은경기 0, gamesPlayed 3 → isClinched true
            val tigersResult = result.find { it.teamId == 1L }!!
            assertThat(tigersResult.isClinched).isTrue()
            // Lions(2위) - isEliminated true(maxPossibleWins=0 < 3)
            val lionsResult = result.find { it.teamId == 2L }!!
            assertThat(lionsResult.isEliminated).isTrue()
        }

        @Test
        fun `비선두팀 rawMagicNumber 0 이하이고 탈락 아닌 경우 isClinched true`() {
            // given: 3팀, 2팀이 플레이오프 진출, 3위팀의 rawMagicNumber <= 0이 되는 상황
            // Tigers: 10승 0패, Lions: 9승 1패, Bears: 0승 10패
            // leader(Tigers) remainingGames = 0
            // Bears: rawMN = 0+1-(10-0) = -9 <= 0, maxPossibleWins = 0+0 = 0 < 10 → isEliminated true
            // Lions: rawMN = 0+1-(10-9) = 0 <= 0, maxPossibleWins = 9+0=9 < 10 → isEliminated true
            // → Lions도 isEliminated=true이면 isClinched = false
            // Let's try: Tigers 3승, Lions 2승, Bears 0승, no remaining
            // Bears: rawMN = 0+1-(3-0) = -2 <=0, maxPossible = 0 < 3 → isEliminated true → isClinched false
            // Lions: rawMN = 0+1-(3-2) = 0 <=0, maxPossible = 2+0=2 < 3 → isEliminated true → isClinched false
            //
            // To get isClinched=true for a non-leader, we need:
            // rawMagicNumber <= 0 && !isEliminated
            // maxPossibleWins >= leader.wins but rawMN <= 0
            // Scenario: Tigers 5승 1패, Lions 3승 1패, Lions remainingGames=2
            // leader = Tigers (5승), remainingGames=0
            // Lions: rawMN = 0+1-(5-3) = -1 <= 0
            //        maxPossibleWins = 3+2 = 5 >= 5 → isEliminated = false
            //        isClinched = true

            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            // Tigers: 5 decided games (WIN), 0 remaining
            val tigerGames = (1..5).map { createGame(it.toLong(), competition) }
            // Lions: 3 decided games (WIN, LOSS), 2 remaining
            val lionGames = (1..3).map { createGame(it.toLong(), competition) }
            val lionRemainingGames = (6..7).map { createGame(it.toLong(), competition) }

            val decidedGameTeams = mutableListOf<GameTeam>()
            val allGameTeams = mutableListOf<GameTeam>()

            var gtId = 1L
            tigerGames.forEachIndexed { i, game ->
                val gt = createGameTeam(gtId++, game, teamA, HomeAway.HOME, 5, GameResult.WIN)
                val gl = createGameTeam(gtId++, game, teamB, HomeAway.AWAY, 2, GameResult.LOSS)
                decidedGameTeams += listOf(gt, gl)
                allGameTeams += listOf(gt, gl)
            }

            // override Lions in Tiger's games - actually need separate games per team
            // Simpler: separate games for Lions vs some dummy
            // Reset and use a cleaner scenario:
            // Tigers play 5 games against external (only Tigers in allGameTeams for those games)
            // Let's just use 2 teams playing each other

            // Clean approach: Tigers 5 wins (5 games), Lions: 3 wins from same 5 games → impossible if they play each other

            // Best approach: Tigers 5W vs unknown, Lions 3W vs unknown
            // Use single-team game entries (no opponent in allGameTeams → opponent not tracked)
            decidedGameTeams.clear()
            allGameTeams.clear()

            val tigerOnlyGames = (10..14).map { createGame(it.toLong(), competition) }
            val lionOnlyGamesDecided = (20..22).map { createGame(it.toLong(), competition) }
            val lionOnlyGamesRemaining = (30..31).map { createGame(it.toLong(), competition) }

            var id = 1L
            tigerOnlyGames.forEach { game ->
                val gt = createGameTeam(id++, game, teamA, HomeAway.HOME, 5, GameResult.WIN)
                decidedGameTeams += gt
                allGameTeams += gt
            }
            lionOnlyGamesDecided.forEach { game ->
                val gl = createGameTeam(id++, game, teamB, HomeAway.HOME, 4, GameResult.WIN)
                decidedGameTeams += gl
                allGameTeams += gl
            }
            lionOnlyGamesRemaining.forEach { game ->
                val gl = createGameTeam(id++, game, teamB, HomeAway.HOME, 0, GameResult.UNDECIDED)
                allGameTeams += gl
            }

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = service.calculateMagicNumbers(1L)

            // then: Tigers leader, remainingGames=0, gamesPlayed=5 → isClinched=true
            val tigersResult = result.find { it.teamId == 1L }!!
            assertThat(tigersResult.isClinched).isTrue()

            // Lions: rawMN = 0+1-(5-3) = -1 <= 0, maxPossibleWins = 3+2=5 >= 5 → isEliminated=false → isClinched=true
            val lionsResult = result.find { it.teamId == 2L }!!
            assertThat(lionsResult.isClinched).isTrue()
            assertThat(lionsResult.isEliminated).isFalse()
        }

        @Test
        fun `팀이 한 경기도 없는 경우 winningPercentage 0`() {
            // gamesPlayed == 0 → BigDecimal.ZERO branch in buildTeamStatsMap
            val teamA = createTeam(1L, league, "Tigers")
            val game1 = createGame(1L, competition)
            // Only UNDECIDED games → decided is empty
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns listOf(gt1)
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns emptyList()

            // when
            val result = service.calculateMagicNumbers(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].teamId).isEqualTo(1L)
            assertThat(result[0].magicNumber).isGreaterThanOrEqualTo(0)
        }
    }

    @Nested
    @DisplayName("simulateStandings - 추가 시나리오")
    inner class SimulateStandingsExtra {
        @Test
        fun `홈팀 승리 시나리오가 올바르게 적용된다`() {
            // given: home win path (simResult.homeScore > simResult.awayScore)
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val game2 = createGame(2L, competition)

            // 현재 동률
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)
            val gt2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val gl2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 0, GameResult.UNDECIDED)

            val allGameTeams = listOf(gt1, gl1, gt2, gl2)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when: Tigers(홈)이 game2에서 승리
            val request =
                SimulationRequest(
                    listOf(SimulatedGameResult(gameId = 2L, homeScore = 5, awayScore = 1)),
                )

            val result = service.simulateStandings(1L, request)

            // then: Tigers 2승, Lions 0승 1패 → Tigers 1위
            assertThat(result.standings).hasSize(2)
            val tigers = result.standings.find { it.teamId == 1L }!!
            assertThat(tigers.wins).isEqualTo(2)
            assertThat(tigers.rank).isEqualTo(1)
        }

        @Test
        fun `존재하지 않는 gameId 시뮬레이션은 무시된다`() {
            // given: gameId가 allGameTeams에 없는 경우 → return@forEach
            val teamA = createTeam(1L, league, "Tigers")
            val game1 = createGame(1L, competition)
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns listOf(gt1)
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns listOf(gt1)

            // when: 존재하지 않는 gameId 999
            val request =
                SimulationRequest(
                    listOf(SimulatedGameResult(gameId = 999L, homeScore = 5, awayScore = 1)),
                )

            val result = service.simulateStandings(1L, request)

            // then: 결과에 변화 없음
            assertThat(result.standings).hasSize(1)
            assertThat(result.changes).isEmpty()
        }

        @Test
        fun `awayScore가 음수이면 InvalidInputException 발생`() {
            // given
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns emptyList()
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns emptyList()

            val request =
                SimulationRequest(
                    listOf(SimulatedGameResult(gameId = 1L, homeScore = 3, awayScore = -1)),
                )

            // when/then
            assertThrows<InvalidInputException> {
                service.simulateStandings(1L, request)
            }
        }
    }

    @Nested
    @DisplayName("calculatePlayoffScenarios - 몬테카를로 시뮬레이션")
    inner class CalculatePlayoffScenariosMonteCarlo {
        @Test
        fun `남은 경기가 15 초과이면 몬테카를로 시뮬레이션을 수행한다`() {
            // given: 2팀, 16개의 미결 경기 → MONTE_CARLO_THRESHOLD(15) 초과
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            // 1 결정된 경기
            val decidedGame = createGame(1L, competition)
            val gt1 = createGameTeam(1L, decidedGame, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, decidedGame, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            val decidedGameTeams = listOf(gt1, gl1)
            val allGameTeams = mutableListOf<GameTeam>()
            allGameTeams += gt1
            allGameTeams += gl1

            // 16개 미결 경기 추가
            var id = 3L
            (2..17).forEach { gameNum ->
                val game = createGame(gameNum.toLong(), competition)
                val gtUndecided =
                    createGameTeam(id++, game, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
                val glUndecided =
                    createGameTeam(id++, game, teamB, HomeAway.AWAY, 0, GameResult.UNDECIDED)
                allGameTeams += gtUndecided
                allGameTeams += glUndecided
            }

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = service.calculatePlayoffScenarios(1L, 1L, 2)

            // then: 몬테카를로 결과 (총 1000 iterations)
            assertThat(result.totalScenarios).isEqualTo(1000)
            assertThat(result.qualifyingScenarios).isBetween(0, 1000)
            assertThat(result.probability).isBetween(0.0, 1.0)
        }

        @Test
        fun `남은 경기가 있을 때 완전 탐색 시뮬레이션 내부 루프가 실행된다`() {
            // given: 2팀, 2개의 미결 경기 (완전 탐색, 3^2=9 시나리오)
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val decidedGame = createGame(1L, competition)
            val gt1 = createGameTeam(1L, decidedGame, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, decidedGame, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            val game2 = createGame(2L, competition)
            val game3 = createGame(3L, competition)
            val gt2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val gl2 = createGameTeam(4L, game2, teamB, HomeAway.AWAY, 0, GameResult.UNDECIDED)
            val gt3 = createGameTeam(5L, game3, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val gl3 = createGameTeam(6L, game3, teamB, HomeAway.AWAY, 0, GameResult.UNDECIDED)

            val allGameTeams = listOf(gt1, gl1, gt2, gl2, gt3, gl3)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when: Tigers의 플레이오프 시나리오 (상위 1팀)
            val result = service.calculatePlayoffScenarios(1L, 1L, 1)

            // then: 3^2 = 9 시나리오
            assertThat(result.totalScenarios).isEqualTo(9)
            assertThat(result.qualifyingScenarios).isBetween(0, 9)
            assertThat(result.probability).isBetween(0.0, 1.0)
        }

        @Test
        fun `남은 경기가 있고 상위 2팀 플레이오프 진출 가능성이 계산된다`() {
            // given: 3팀, 2개 미결 경기
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")
            val teamC = createTeam(3L, league, "Bears")

            val game1 = createGame(1L, competition)
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            val game2 = createGame(2L, competition)
            val gt2 = createGameTeam(3L, game2, teamA, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val gl2 = createGameTeam(4L, game2, teamC, HomeAway.AWAY, 0, GameResult.UNDECIDED)

            val game3 = createGame(3L, competition)
            val gt3 = createGameTeam(5L, game3, teamB, HomeAway.HOME, 0, GameResult.UNDECIDED)
            val gl3 = createGameTeam(6L, game3, teamC, HomeAway.AWAY, 0, GameResult.UNDECIDED)

            val allGameTeams = listOf(gt1, gl1, gt2, gl2, gt3, gl3)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when: Bears 플레이오프(상위 2팀)
            val result = service.calculatePlayoffScenarios(1L, 3L, 2)

            // then: 3^2=9 시나리오
            assertThat(result.totalScenarios).isEqualTo(9)
            assertThat(result.probability).isBetween(0.0, 1.0)
        }

        @Test
        fun `모든 경기 완료 후 완전 탐색은 1개 시나리오만 실행`() {
            // given: 남은 경기 없음 → totalScenarios = 3^0 = 1
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 5, GameResult.WIN)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 2, GameResult.LOSS)

            val allGameTeams = listOf(gt1, gl1)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val result = service.calculatePlayoffScenarios(1L, 1L, 2)

            // then: totalScenarios = 1, Tigers는 1위이므로 probability = 1.0
            assertThat(result.totalScenarios).isEqualTo(1)
            assertThat(result.probability).isEqualTo(1.0)
        }

        @Test
        fun `무승부 경기가 있는 경우 draws 카운트가 올바르다`() {
            // given: 무승부 결과 포함 → draws branch in buildTeamStatsMap
            val teamA = createTeam(1L, league, "Tigers")
            val teamB = createTeam(2L, league, "Lions")

            val game1 = createGame(1L, competition)
            val gt1 = createGameTeam(1L, game1, teamA, HomeAway.HOME, 3, GameResult.DRAW)
            val gl1 = createGameTeam(2L, game1, teamB, HomeAway.AWAY, 3, GameResult.DRAW)

            val allGameTeams = listOf(gt1, gl1)
            val decidedGameTeams = listOf(gt1, gl1)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns allGameTeams
            every { gameTeamRepository.findAllByCompetitionIdWithDecidedResult(1L) } returns decidedGameTeams

            // when
            val magicNumbers = service.calculateMagicNumbers(1L)

            // then
            assertThat(magicNumbers).hasSize(2)
            // 무승부인 경우 winningPercentage = 0.5
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private fun createAssociation(
        id: Long,
        name: String,
    ): Association =
        Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null,
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association,
    ): League =
        League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null,
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createCompetition(
        id: Long,
        league: League,
        name: String,
        year: Int,
        season: Int,
    ): Competition =
        Competition(
            league = league,
            name = name,
            year = year,
            season = season,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(year, 3, 1),
            endDate = null,
            description = null,
            maxTeams = null,
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createTeam(
        id: Long,
        league: League,
        name: String,
    ): Team =
        Team(
            league = league,
            name = name,
            city = "서울",
            foundedYear = 2020,
            abbreviation = null,
            logoUrl = null,
        ).apply {
            val idField = Team::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createGame(
        id: Long,
        competition: Competition,
    ): Game =
        Game(
            competition = competition,
            scheduledAt = LocalDateTime.now(),
            location = "서울야구장",
            status = GameStatus.SCHEDULED,
        ).apply {
            val idField = Game::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createGameTeam(
        id: Long,
        game: Game,
        team: Team,
        homeAway: HomeAway,
        totalScore: Int,
        result: GameResult,
    ): GameTeam =
        GameTeam(
            game = game,
            team = team,
            homeAway = homeAway,
            id = id,
        ).apply {
            updateScore(totalScore, 0, 0)
            updateResult(result)
        }
}
