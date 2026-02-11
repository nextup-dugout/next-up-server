package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.stats.dto.BattingCategory
import com.nextup.core.service.stats.dto.PitchingCategory
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("IndividualRankingServiceImpl")
class IndividualRankingServiceImplTest {
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort
    private lateinit var seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var service: IndividualRankingServiceImpl

    private lateinit var competition: Competition
    private lateinit var player1: Player
    private lateinit var player2: Player
    private lateinit var team: Team

    @BeforeEach
    fun setUp() {
        competitionRepository = mockk()
        seasonBattingStatsRepository = mockk()
        seasonPitchingStatsRepository = mockk()
        teamMemberRepository = mockk()

        service =
            IndividualRankingServiceImpl(
                competitionRepository,
                seasonBattingStatsRepository,
                seasonPitchingStatsRepository,
                teamMemberRepository,
            )

        val league =
            mockk<League> {
                every { id } returns 1L
                every { name } returns "1부 리그"
            }

        competition =
            Competition(
                league = league,
                name = "2026 춘계대회",
                year = 2026,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2026, 3, 1),
            )
        setId(competition, Competition::class.java, 1L)

        player1 = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP)
        setId(player1, Player::class.java, 10L)

        player2 = Player(name = "김철수", primaryPosition = Position.CENTER_FIELD)
        setId(player2, Player::class.java, 20L)

        team =
            mockk<Team> {
                every { id } returns 100L
                every { name } returns "타이거즈"
            }
    }

    @Nested
    @DisplayName("getBattingLeaders")
    inner class GetBattingLeaders {
        @Test
        fun `should throw when competition not found`() {
            // given
            every { competitionRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.getBattingLeaders(999L, BattingCategory.BATTING_AVG)
            }.isInstanceOf(CompetitionNotFoundException::class.java)
        }

        @Test
        fun `should return batting average leaders`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 18, pa = 55, games = 12)
            val stats2 = createBattingStats(player2, 2026, atBats = 45, hits = 14, pa = 50, games = 10)
            val qualifyingPA =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_PA_FACTOR).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, qualifyingPA, 10) } returns
                listOf(stats1, stats2)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))
            every { teamMemberRepository.findByPlayerIdActive(20L) } returns listOf(createTeamMember(player2))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.BATTING_AVG)

            // then
            assertThat(result).hasSize(2)
            assertThat(result[0].rank).isEqualTo(1)
            assertThat(result[0].playerName).isEqualTo("홍길동")
            assertThat(result[0].teamName).isEqualTo("타이거즈")
            assertThat(result[1].rank).isEqualTo(2)
            assertThat(result[1].playerName).isEqualTo("김철수")
        }

        @Test
        fun `should return home run leaders`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 15, pa = 55, games = 12, homeRuns = 5)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.HOME_RUNS)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].value).isEqualTo(5.0)
        }

        @Test
        fun `should return RBI leaders`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 15, pa = 55, games = 12, rbi = 20)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.RBI)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].value).isEqualTo(20.0)
        }

        @Test
        fun `should return hits leaders`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 25, pa = 55, games = 12)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByHits(2026, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.HITS)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].value).isEqualTo(25.0)
        }

        @Test
        fun `should return stolen bases leaders`() {
            // given
            val stats1 =
                createBattingStats(player1, 2026, atBats = 50, hits = 15, pa = 55, games = 12, stolenBases = 10)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.STOLEN_BASES)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].value).isEqualTo(10.0)
        }

        @Test
        fun `should return OBP leaders`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 15, pa = 55, games = 12)
            val qualifyingPA =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_PA_FACTOR).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByOnBasePercentage(2026, qualifyingPA, 10) } returns
                listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.OBP)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        fun `should return SLG leaders`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 15, pa = 55, games = 12)
            val qualifyingPA =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_PA_FACTOR).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopBySlugging(2026, qualifyingPA, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.SLG)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        fun `should return OPS leaders`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 15, pa = 55, games = 12)
            val qualifyingPA =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_PA_FACTOR).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByOps(2026, qualifyingPA, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.OPS)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        fun `should return empty list when no stats`() {
            // given
            val qualifyingPA =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_PA_FACTOR).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, qualifyingPA, 10) } returns emptyList()

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.BATTING_AVG)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `should return dash for team name when player has no active team`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 18, pa = 55, games = 12)
            val qualifyingPA =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_PA_FACTOR).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, qualifyingPA, 10) } returns
                listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns emptyList()

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.BATTING_AVG)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].teamName).isEqualTo("-")
        }

        @Test
        fun `should support custom limit`() {
            // given
            val stats1 = createBattingStats(player1, 2026, atBats = 50, hits = 18, pa = 55, games = 12)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 5) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getBattingLeaders(1L, BattingCategory.HOME_RUNS, limit = 5)

            // then
            assertThat(result).hasSize(1)
        }
    }

    @Nested
    @DisplayName("getPitchingLeaders")
    inner class GetPitchingLeaders {
        @Test
        fun `should throw when competition not found`() {
            // given
            every { competitionRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.getPitchingLeaders(999L, PitchingCategory.ERA)
            }.isInstanceOf(CompetitionNotFoundException::class.java)
        }

        @Test
        fun `should return ERA leaders`() {
            // given
            val stats1 = createPitchingStats(player1, 2026, ipOuts = 36, earnedRuns = 3, games = 10)
            val stats2 = createPitchingStats(player2, 2026, ipOuts = 30, earnedRuns = 5, games = 8)
            val qualifyingIPOuts =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_IP_FACTOR *
                    3).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonPitchingStatsRepository.findTopByEra(2026, qualifyingIPOuts, 10) } returns
                listOf(stats1, stats2)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))
            every { teamMemberRepository.findByPlayerIdActive(20L) } returns listOf(createTeamMember(player2))

            // when
            val result = service.getPitchingLeaders(1L, PitchingCategory.ERA)

            // then
            assertThat(result).hasSize(2)
            assertThat(result[0].rank).isEqualTo(1)
            assertThat(result[0].playerName).isEqualTo("홍길동")
            assertThat(result[0].teamName).isEqualTo("타이거즈")
            assertThat(result[1].rank).isEqualTo(2)
        }

        @Test
        fun `should return wins leaders`() {
            // given
            val stats1 = createPitchingStats(player1, 2026, ipOuts = 50, earnedRuns = 10, games = 12, wins = 8)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonPitchingStatsRepository.findTopByWins(2026, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getPitchingLeaders(1L, PitchingCategory.WINS)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].value).isEqualTo(8.0)
        }

        @Test
        fun `should return saves leaders`() {
            // given
            val stats1 = createPitchingStats(player1, 2026, ipOuts = 30, earnedRuns = 2, games = 15, saves = 10)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getPitchingLeaders(1L, PitchingCategory.SAVES)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].value).isEqualTo(10.0)
        }

        @Test
        fun `should return strikeout leaders`() {
            // given
            val stats1 = createPitchingStats(player1, 2026, ipOuts = 50, earnedRuns = 10, games = 12, strikeouts = 45)

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getPitchingLeaders(1L, PitchingCategory.STRIKEOUTS)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].value).isEqualTo(45.0)
        }

        @Test
        fun `should return WHIP leaders`() {
            // given
            val stats1 = createPitchingStats(player1, 2026, ipOuts = 36, earnedRuns = 3, games = 10)
            val qualifyingIPOuts =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_IP_FACTOR *
                    3).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonPitchingStatsRepository.findTopByWhip(2026, qualifyingIPOuts, 10) } returns listOf(stats1)
            every { teamMemberRepository.findByPlayerIdActive(10L) } returns listOf(createTeamMember(player1))

            // when
            val result = service.getPitchingLeaders(1L, PitchingCategory.WHIP)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        fun `should return empty list when no pitching stats`() {
            // given
            val qualifyingIPOuts =
                (IndividualRankingServiceImpl.DEFAULT_TEAM_GAMES *
                    IndividualRankingServiceImpl.QUALIFYING_IP_FACTOR *
                    3).toInt()

            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { seasonPitchingStatsRepository.findTopByEra(2026, qualifyingIPOuts, 10) } returns emptyList()

            // when
            val result = service.getPitchingLeaders(1L, PitchingCategory.ERA)

            // then
            assertThat(result).isEmpty()
        }
    }

    // Helper methods

    private fun createBattingStats(
        player: Player,
        year: Int,
        atBats: Int = 0,
        hits: Int = 0,
        pa: Int = 0,
        games: Int = 0,
        homeRuns: Int = 0,
        rbi: Int = 0,
        stolenBases: Int = 0,
    ): SeasonBattingStats {
        val stats = SeasonBattingStats(player = player, year = year)
        setField(stats, "gamesPlayed", games)
        setField(stats, "plateAppearances", pa)
        setField(stats, "atBats", atBats)
        setField(stats, "hits", hits)
        setField(stats, "homeRuns", homeRuns)
        setField(stats, "runsBattedIn", rbi)
        setField(stats, "stolenBases", stolenBases)
        return stats
    }

    private fun createPitchingStats(
        player: Player,
        year: Int,
        ipOuts: Int = 0,
        earnedRuns: Int = 0,
        games: Int = 0,
        wins: Int = 0,
        saves: Int = 0,
        strikeouts: Int = 0,
    ): SeasonPitchingStats {
        val stats = SeasonPitchingStats(player = player, year = year)
        setField(stats, "gamesPlayed", games)
        setField(stats, "inningsPitchedOuts", ipOuts)
        setField(stats, "earnedRuns", earnedRuns)
        setField(stats, "runsAllowed", earnedRuns)
        setField(stats, "wins", wins)
        setField(stats, "saves", saves)
        setField(stats, "strikeouts", strikeouts)
        setField(stats, "hitsAllowed", 0)
        setField(stats, "walksAllowed", 0)
        setField(stats, "battersFaced", 0)
        return stats
    }

    private fun createTeamMember(player: Player): TeamMember {
        val member =
            mockk<TeamMember> {
                every { this@mockk.team } returns this@IndividualRankingServiceImplTest.team
                every { role } returns TeamMemberRole.MEMBER
            }
        return member
    }

    private fun setField(
        target: Any,
        fieldName: String,
        value: Any
    ) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun <T> setId(
        target: T,
        clazz: Class<T>,
        id: Long
    ) {
        val idField = clazz.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(target, id)
    }
}
