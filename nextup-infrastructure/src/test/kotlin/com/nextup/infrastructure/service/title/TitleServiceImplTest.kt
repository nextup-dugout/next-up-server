package com.nextup.infrastructure.service.title

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import com.nextup.core.service.title.TitleCategory
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("TitleServiceImpl")
class TitleServiceImplTest {
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort
    private lateinit var seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort
    private lateinit var titleService: TitleServiceImpl

    private lateinit var competition: Competition
    private lateinit var league: League
    private lateinit var association: Association

    @BeforeEach
    fun setUp() {
        competitionRepository = mockk()
        gameTeamRepository = mockk()
        seasonBattingStatsRepository = mockk()
        seasonPitchingStatsRepository = mockk()
        titleService =
            TitleServiceImpl(
                competitionRepository,
                gameTeamRepository,
                seasonBattingStatsRepository,
                seasonPitchingStatsRepository,
            )

        // Test data setup
        association = createAssociation(1L, "서울시야구협회")
        league = createLeague(1L, "1부 리그", association)
        competition = createCompetition(1L, league, "2025 춘계대회", 2025, 1)
    }

    @Nested
    @DisplayName("getTitles")
    inner class GetTitles {
        @Test
        fun `대회가 존재하지 않으면 CompetitionNotFoundException 발생`() {
            // given
            val competitionId = 999L
            every { competitionRepository.findByIdOrNull(competitionId) } returns null

            // when & then
            assertThrows<CompetitionNotFoundException> {
                titleService.getTitles(competitionId)
            }
        }

        @Test
        fun `모든 타이틀 카테고리를 반환`() {
            // given
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns emptyList()
            every { seasonBattingStatsRepository.findAllByYear(2025) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2025, 10) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2025, 10) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2025, 10) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2025, 10) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2025, 10) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByYear(2025) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2025, 10) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2025, 10) } returns emptyList()

            // when
            val result = titleService.getTitles(1L)

            // then
            assertThat(result).hasSize(9) // 9개의 타이틀 카테고리
            assertThat(result.map { it.category }).containsExactlyInAnyOrder(
                TitleCategory.BATTING_AVG,
                TitleCategory.HOME_RUNS,
                TitleCategory.RBI,
                TitleCategory.STOLEN_BASES,
                TitleCategory.HITS,
                TitleCategory.WINS,
                TitleCategory.ERA,
                TitleCategory.SAVES,
                TitleCategory.STRIKEOUTS,
            )
        }
    }

    @Nested
    @DisplayName("getTitleByCategory - 타격왕")
    inner class GetBattingAvgTitle {
        @Test
        fun `규정 타석 충족 선수를 1위로 선정`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val game1 = createGame(1L, competition)

            // 팀당 10경기 (규정 타석 = 10 * 3.1 = 31타석)
            val gameTeams = (1..10).map { createGameTeam(it.toLong(), game1, teamA, HomeAway.HOME, 0) }
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns gameTeams

            val player1 = createPlayer(1L, "김타격")
            val player2 = createPlayer(2L, "박안타")

            // player1: 타율 .400 (40타석, 규정 충족)
            val stats1 = createBattingStats(1L, player1, 2025, plateAppearances = 40, atBats = 35, hits = 14)
            // player2: 타율 .500 (20타석, 규정 미충족)
            val stats2 = createBattingStats(2L, player2, 2025, plateAppearances = 20, atBats = 18, hits = 9)

            every { seasonBattingStatsRepository.findAllByYear(2025) } returns listOf(stats2, stats1)

            // when
            val result = titleService.getTitleByCategory(1L, TitleCategory.BATTING_AVG)

            // then
            assertThat(result.category).isEqualTo(TitleCategory.BATTING_AVG)
            assertThat(result.displayName).isEqualTo("타격왕")
            assertThat(result.winner).isNotNull
            assertThat(result.winner?.playerId).isEqualTo(1L) // 규정 충족한 player1이 우승
            assertThat(result.winner?.playerName).isEqualTo("김타격")
            assertThat(result.topCandidates).hasSize(2)
            assertThat(result.topCandidates[0].rank).isEqualTo(1)
            assertThat(result.topCandidates[0].isQualified).isFalse() // 타율 1위지만 규정 미충족
            assertThat(result.topCandidates[1].rank).isEqualTo(2)
            assertThat(result.topCandidates[1].isQualified).isTrue() // 규정 충족
        }

        @Test
        fun `규정 타석 충족 선수가 없으면 우승자 없음`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val game1 = createGame(1L, competition)
            val gameTeams = (1..10).map { createGameTeam(it.toLong(), game1, teamA, HomeAway.HOME, 0) }

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns gameTeams

            val player1 = createPlayer(1L, "김타격")
            // 규정 타석 미충족 (31타석 필요, 20타석만 기록)
            val stats1 = createBattingStats(1L, player1, 2025, plateAppearances = 20, atBats = 18, hits = 9)

            every { seasonBattingStatsRepository.findAllByYear(2025) } returns listOf(stats1)

            // when
            val result = titleService.getTitleByCategory(1L, TitleCategory.BATTING_AVG)

            // then
            assertThat(result.winner).isNull()
            assertThat(result.topCandidates).hasSize(1)
            assertThat(result.topCandidates[0].isQualified).isFalse()
        }
    }

    @Nested
    @DisplayName("getTitleByCategory - 홈런왕")
    inner class GetHomeRunsTitle {
        @Test
        fun `홈런 최다 선수를 우승자로 선정`() {
            // given
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns emptyList()

            val player1 = createPlayer(1L, "김장타")
            val player2 = createPlayer(2L, "박홈런")

            val stats1 = createBattingStats(1L, player1, 2025, homeRuns = 15)
            val stats2 = createBattingStats(2L, player2, 2025, homeRuns = 12)

            every { seasonBattingStatsRepository.findTopByHomeRuns(2025, 10) } returns listOf(stats1, stats2)

            // when
            val result = titleService.getTitleByCategory(1L, TitleCategory.HOME_RUNS)

            // then
            assertThat(result.category).isEqualTo(TitleCategory.HOME_RUNS)
            assertThat(result.winner).isNotNull
            assertThat(result.winner?.playerId).isEqualTo(1L)
            assertThat(result.winner?.statValue).isEqualTo(15.0)
            assertThat(result.topCandidates).hasSize(2)
            assertThat(result.topCandidates[0].isQualified).isTrue() // 홈런왕은 규정 타석 불필요
        }
    }

    @Nested
    @DisplayName("getTitleByCategory - 평균자책점")
    inner class GetEraTitle {
        @Test
        fun `규정 이닝 충족 투수 중 ERA 최저 선수를 우승자로 선정`() {
            // given
            val teamA = createTeam(1L, league, "Tigers")
            val game1 = createGame(1L, competition)

            // 팀당 10경기 (규정 이닝 = 10 * 1.0 = 10이닝 = 30아웃)
            val gameTeams = (1..10).map { createGameTeam(it.toLong(), game1, teamA, HomeAway.HOME, 0) }
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns gameTeams

            val player1 = createPlayer(1L, "김투수")
            val player2 = createPlayer(2L, "박마운드")

            // player1: ERA 2.00 (45아웃 = 15이닝, 규정 충족)
            val stats1 = createPitchingStats(1L, player1, 2025, inningsPitchedOuts = 45, earnedRuns = 10)
            // player2: ERA 1.50 (18아웃 = 6이닝, 규정 미충족)
            val stats2 = createPitchingStats(2L, player2, 2025, inningsPitchedOuts = 18, earnedRuns = 3)

            every { seasonPitchingStatsRepository.findAllByYear(2025) } returns listOf(stats2, stats1)

            // when
            val result = titleService.getTitleByCategory(1L, TitleCategory.ERA)

            // then
            assertThat(result.category).isEqualTo(TitleCategory.ERA)
            assertThat(result.winner).isNotNull
            assertThat(result.winner?.playerId).isEqualTo(1L) // 규정 충족한 player1이 우승
            assertThat(result.topCandidates).hasSize(2)
            assertThat(result.topCandidates[0].rank).isEqualTo(1)
            assertThat(result.topCandidates[0].isQualified).isFalse() // ERA 1위지만 규정 미충족
            assertThat(result.topCandidates[1].rank).isEqualTo(2)
            assertThat(result.topCandidates[1].isQualified).isTrue() // 규정 충족
        }
    }

    @Nested
    @DisplayName("getTitleByCategory - 다승왕")
    inner class GetWinsTitle {
        @Test
        fun `승수 최다 투수를 우승자로 선정`() {
            // given
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { gameTeamRepository.findAllByCompetitionId(1L) } returns emptyList()

            val player1 = createPlayer(1L, "김에이스")
            val player2 = createPlayer(2L, "박선발")

            val stats1 = createPitchingStats(1L, player1, 2025, wins = 12)
            val stats2 = createPitchingStats(2L, player2, 2025, wins = 10)

            every { seasonPitchingStatsRepository.findTopByWins(2025, 10) } returns listOf(stats1, stats2)

            // when
            val result = titleService.getTitleByCategory(1L, TitleCategory.WINS)

            // then
            assertThat(result.category).isEqualTo(TitleCategory.WINS)
            assertThat(result.winner).isNotNull
            assertThat(result.winner?.playerId).isEqualTo(1L)
            assertThat(result.winner?.statValue).isEqualTo(12.0)
            assertThat(result.topCandidates[0].isQualified).isTrue() // 다승왕은 규정 이닝 불필요
        }
    }

    // ========== Helper Methods ==========

    private fun createAssociation(
        id: Long,
        name: String,
    ): Association {
        val association = mockk<Association>(relaxed = true)
        every { association.id } returns id
        every { association.name } returns name
        return association
    }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association,
    ): League {
        val league = mockk<League>(relaxed = true)
        every { league.id } returns id
        every { league.name } returns name
        every { league.association } returns association
        return league
    }

    private fun createCompetition(
        id: Long,
        league: League,
        name: String,
        year: Int,
        season: Int,
    ): Competition {
        val competition = mockk<Competition>(relaxed = true)
        every { competition.id } returns id
        every { competition.league } returns league
        every { competition.name } returns name
        every { competition.year } returns year
        every { competition.season } returns season
        every { competition.type } returns CompetitionType.LEAGUE
        every { competition.startDate } returns LocalDate.of(year, 3, 1)
        return competition
    }

    private fun createTeam(
        id: Long,
        league: League,
        name: String,
    ): Team {
        val team = mockk<Team>(relaxed = true)
        every { team.id } returns id
        every { team.league } returns league
        every { team.name } returns name
        return team
    }

    private fun createGame(
        id: Long,
        competition: Competition,
    ): Game {
        val game = mockk<Game>(relaxed = true)
        every { game.id } returns id
        every { game.competition } returns competition
        every { game.status } returns GameStatus.FINISHED
        every { game.scheduledAt } returns LocalDateTime.of(2025, 3, 15, 14, 0)
        return game
    }

    private fun createGameTeam(
        id: Long,
        game: Game,
        team: Team,
        homeAway: HomeAway,
        totalScore: Int,
    ): GameTeam {
        val gameTeam = mockk<GameTeam>(relaxed = true)
        every { gameTeam.id } returns id
        every { gameTeam.game } returns game
        every { gameTeam.team } returns team
        every { gameTeam.homeAway } returns homeAway
        every { gameTeam.totalScore } returns totalScore
        return gameTeam
    }

    private fun createPlayer(
        id: Long,
        name: String,
    ): Player {
        val player = mockk<Player>(relaxed = true)
        every { player.id } returns id
        every { player.name } returns name
        every { player.primaryPosition } returns Position.STARTING_PITCHER
        every { player.throwingHand } returns ThrowingHand.RIGHT
        every { player.battingHand } returns BattingHand.RIGHT
        return player
    }

    private fun createBattingStats(
        id: Long,
        player: Player,
        year: Int,
        plateAppearances: Int = 0,
        atBats: Int = 0,
        hits: Int = 0,
        homeRuns: Int = 0,
    ): SeasonBattingStats {
        val stats = mockk<SeasonBattingStats>(relaxed = true)
        every { stats.id } returns id
        every { stats.player } returns player
        every { stats.year } returns year
        every { stats.plateAppearances } returns plateAppearances
        every { stats.atBats } returns atBats
        every { stats.hits } returns hits
        every { stats.homeRuns } returns homeRuns
        every { stats.battingAverage } returns
            if (atBats > 0) {
                BigDecimal(hits).divide(BigDecimal(atBats), 3, java.math.RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
        return stats
    }

    private fun createPitchingStats(
        id: Long,
        player: Player,
        year: Int,
        inningsPitchedOuts: Int = 0,
        earnedRuns: Int = 0,
        wins: Int = 0,
    ): SeasonPitchingStats {
        val stats = mockk<SeasonPitchingStats>(relaxed = true)
        every { stats.id } returns id
        every { stats.player } returns player
        every { stats.year } returns year
        every { stats.inningsPitchedOuts } returns inningsPitchedOuts
        every { stats.earnedRuns } returns earnedRuns
        every { stats.wins } returns wins
        every { stats.earnedRunAverage } returns
            if (inningsPitchedOuts > 0) {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, java.math.RoundingMode.HALF_UP)
                BigDecimal(earnedRuns)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, java.math.RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
        return stats
    }
}
