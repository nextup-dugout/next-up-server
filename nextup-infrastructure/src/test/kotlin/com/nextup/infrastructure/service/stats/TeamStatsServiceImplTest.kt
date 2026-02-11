package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("TeamStatsServiceImpl 테스트")
class TeamStatsServiceImplTest {
    private lateinit var teamRepositoryPort: TeamRepositoryPort
    private lateinit var gameTeamRepositoryPort: GameTeamRepositoryPort
    private lateinit var battingRecordRepositoryPort: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepositoryPort: PitchingRecordRepositoryPort
    private lateinit var teamStatsService: TeamStatsServiceImpl

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var team: Team

    @BeforeEach
    fun setUp() {
        teamRepositoryPort = mockk()
        gameTeamRepositoryPort = mockk()
        battingRecordRepositoryPort = mockk()
        pitchingRecordRepositoryPort = mockk()
        teamStatsService =
            TeamStatsServiceImpl(
                teamRepositoryPort,
                gameTeamRepositoryPort,
                battingRecordRepositoryPort,
                pitchingRecordRepositoryPort,
            )

        association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "테스트팀", city = "서울", foundedYear = 2020)
    }

    private fun createMockTeam(teamId: Long): Team =
        mockk<Team>(relaxed = true).apply {
            every { id } returns teamId
        }

    private fun createMockGameTeam(
        teamMock: Team,
        gameMock: Game,
        gameResult: GameResult,
    ): GameTeam =
        mockk<GameTeam>(relaxed = true).apply {
            every { team } returns teamMock
            every { game } returns gameMock
            every { result } returns gameResult
        }

    @Nested
    @DisplayName("getTeamStats")
    inner class GetTeamStats {
        @Test
        fun `팀이 존재하지 않으면 예외를 발생시킨다`() {
            // given
            every { teamRepositoryPort.findByIdWithLeague(1L) } returns null

            // when & then
            assertThatThrownBy { teamStatsService.getTeamStats(1L, null, null) }
                .isInstanceOf(TeamNotFoundException::class.java)
        }

        @Test
        fun `경기 기록이 없는 팀의 통계를 조회하면 빈 통계를 반환한다`() {
            // given
            every { teamRepositoryPort.findByIdWithLeague(1L) } returns team
            every { gameTeamRepositoryPort.findAllByTeamId(1L) } returns emptyList()

            // when
            val result = teamStatsService.getTeamStats(1L, null, null)

            // then
            assertThat(result.teamId).isEqualTo(0L)
            assertThat(result.teamName).isEqualTo("테스트팀")
            assertThat(result.record.gamesPlayed).isEqualTo(0)
            assertThat(result.record.wins).isEqualTo(0)
            assertThat(result.record.losses).isEqualTo(0)
            assertThat(result.batting.totalAtBats).isEqualTo(0)
            assertThat(result.pitching.totalInningsPitchedOuts).isEqualTo(0)
        }

        @Test
        fun `연도 필터를 적용하여 팀 통계를 조회할 수 있다`() {
            // given
            every { teamRepositoryPort.findByIdWithLeague(1L) } returns team
            every { gameTeamRepositoryPort.findAllByTeamIdAndYear(1L, 2025) } returns emptyList()

            // when
            val result = teamStatsService.getTeamStats(1L, 2025, null)

            // then
            assertThat(result.year).isEqualTo(2025)
            assertThat(result.record.gamesPlayed).isEqualTo(0)
        }

        @Test
        fun `경기 결과를 집계하여 승률을 계산한다`() {
            // given
            val mockTeam = createMockTeam(1L)

            val game1 = mockk<Game>(relaxed = true).apply { every { id } returns 1L }
            val game2 = mockk<Game>(relaxed = true).apply { every { id } returns 2L }
            val game3 = mockk<Game>(relaxed = true).apply { every { id } returns 3L }

            val gameTeam1 = createMockGameTeam(mockTeam, game1, GameResult.WIN)
            val gameTeam2 = createMockGameTeam(mockTeam, game2, GameResult.LOSS)
            val gameTeam3 = createMockGameTeam(mockTeam, game3, GameResult.WIN)

            every { teamRepositoryPort.findByIdWithLeague(1L) } returns team
            every { gameTeamRepositoryPort.findAllByTeamId(1L) } returns listOf(gameTeam1, gameTeam2, gameTeam3)
            every { battingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, any()) } returns emptyList()
            every { pitchingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, any()) } returns emptyList()

            // when
            val result = teamStatsService.getTeamStats(1L, null, null)

            // then
            assertThat(result.record.gamesPlayed).isEqualTo(3)
            assertThat(result.record.wins).isEqualTo(2)
            assertThat(result.record.losses).isEqualTo(1)
            assertThat(result.record.winningPercentage).isEqualTo(BigDecimal("0.667"))
        }

        @Test
        fun `무승부를 포함한 경기 결과를 집계한다`() {
            // given
            val mockTeam = createMockTeam(1L)

            val game1 = mockk<Game>(relaxed = true).apply { every { id } returns 1L }
            val game2 = mockk<Game>(relaxed = true).apply { every { id } returns 2L }

            val gameTeam1 = createMockGameTeam(mockTeam, game1, GameResult.DRAW)
            val gameTeam2 = createMockGameTeam(mockTeam, game2, GameResult.DRAW)

            every { teamRepositoryPort.findByIdWithLeague(1L) } returns team
            every { gameTeamRepositoryPort.findAllByTeamId(1L) } returns listOf(gameTeam1, gameTeam2)
            every { battingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, any()) } returns emptyList()
            every { pitchingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, any()) } returns emptyList()

            // when
            val result = teamStatsService.getTeamStats(1L, null, null)

            // then
            assertThat(result.record.gamesPlayed).isEqualTo(2)
            assertThat(result.record.draws).isEqualTo(2)
            assertThat(result.record.winningPercentage).isEqualTo(BigDecimal("0.000"))
        }

        @Test
        fun `타격 통계를 계산한다`() {
            // given
            val mockTeam = createMockTeam(1L)
            val game = mockk<Game>(relaxed = true).apply { every { id } returns 1L }
            val gameTeam = createMockGameTeam(mockTeam, game, GameResult.WIN)

            val battingRecord1 =
                mockk<BattingRecord>(relaxed = true).apply {
                    every { atBats } returns 4
                    every { hits } returns 2
                    every { homeRuns } returns 1
                    every { runsBattedIn } returns 2
                    every { runs } returns 1
                    every { totalWalks } returns 1
                    every { hitByPitch } returns 0
                    every { sacrificeFlies } returns 0
                    every { totalBases } returns 5
                }

            val battingRecord2 =
                mockk<BattingRecord>(relaxed = true).apply {
                    every { atBats } returns 3
                    every { hits } returns 1
                    every { homeRuns } returns 0
                    every { runsBattedIn } returns 0
                    every { runs } returns 1
                    every { totalWalks } returns 1
                    every { hitByPitch } returns 1
                    every { sacrificeFlies } returns 1
                    every { totalBases } returns 1
                }

            every { teamRepositoryPort.findByIdWithLeague(1L) } returns team
            every { gameTeamRepositoryPort.findAllByTeamId(1L) } returns listOf(gameTeam)
            every {
                battingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, listOf(1L))
            } returns listOf(battingRecord1, battingRecord2)
            every { pitchingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, listOf(1L)) } returns emptyList()

            // when
            val result = teamStatsService.getTeamStats(1L, null, null)

            // then
            assertThat(result.batting.totalAtBats).isEqualTo(7)
            assertThat(result.batting.totalHits).isEqualTo(3)
            assertThat(result.batting.totalHomeRuns).isEqualTo(1)
            assertThat(result.batting.totalRunsBattedIn).isEqualTo(2)
            assertThat(result.batting.totalRuns).isEqualTo(2)
            assertThat(result.batting.teamBattingAverage).isEqualTo(BigDecimal("0.429"))
        }

        @Test
        fun `투수 통계를 계산한다`() {
            // given
            val mockTeam = createMockTeam(1L)
            val game = mockk<Game>(relaxed = true).apply { every { id } returns 1L }
            val gameTeam = createMockGameTeam(mockTeam, game, GameResult.WIN)

            val pitchingRecord =
                mockk<PitchingRecord>(relaxed = true).apply {
                    every { inningsPitchedOuts } returns 21 // 7이닝
                    every { earnedRuns } returns 3
                    every { strikeouts } returns 8
                    every { walksAllowed } returns 2
                    every { hitsAllowed } returns 6
                }

            every { teamRepositoryPort.findByIdWithLeague(1L) } returns team
            every { gameTeamRepositoryPort.findAllByTeamId(1L) } returns listOf(gameTeam)
            every { battingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, listOf(1L)) } returns emptyList()
            every {
                pitchingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, listOf(1L))
            } returns listOf(pitchingRecord)

            // when
            val result = teamStatsService.getTeamStats(1L, null, null)

            // then
            assertThat(result.pitching.totalInningsPitchedOuts).isEqualTo(21)
            assertThat(result.pitching.inningsPitchedDisplay).isEqualTo("7.0")
            assertThat(result.pitching.totalEarnedRuns).isEqualTo(3)
            assertThat(result.pitching.totalStrikeouts).isEqualTo(8)
            assertThat(result.pitching.teamEra).isEqualTo(BigDecimal("3.86"))
            assertThat(result.pitching.teamWhip).isEqualTo(BigDecimal("1.14"))
        }

        @Test
        fun `이닝이 0일 때 투수 통계는 0을 반환한다`() {
            // given
            val mockTeam = createMockTeam(1L)
            val game = mockk<Game>(relaxed = true).apply { every { id } returns 1L }
            val gameTeam = createMockGameTeam(mockTeam, game, GameResult.WIN)

            val pitchingRecord =
                mockk<PitchingRecord>(relaxed = true).apply {
                    every { inningsPitchedOuts } returns 0
                    every { earnedRuns } returns 0
                    every { strikeouts } returns 0
                    every { walksAllowed } returns 0
                    every { hitsAllowed } returns 0
                }

            every { teamRepositoryPort.findByIdWithLeague(1L) } returns team
            every { gameTeamRepositoryPort.findAllByTeamId(1L) } returns listOf(gameTeam)
            every { battingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, listOf(1L)) } returns emptyList()
            every {
                pitchingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, listOf(1L))
            } returns listOf(pitchingRecord)

            // when
            val result = teamStatsService.getTeamStats(1L, null, null)

            // then
            assertThat(result.pitching.teamEra).isEqualTo(BigDecimal("0.00"))
            assertThat(result.pitching.teamWhip).isEqualTo(BigDecimal("0.00"))
        }

        @Test
        fun `타석이 0일 때 타격 통계는 0을 반환한다`() {
            // given
            val mockTeam = createMockTeam(1L)
            val game = mockk<Game>(relaxed = true).apply { every { id } returns 1L }
            val gameTeam = createMockGameTeam(mockTeam, game, GameResult.WIN)

            val battingRecord =
                mockk<BattingRecord>(relaxed = true).apply {
                    every { atBats } returns 0
                    every { hits } returns 0
                    every { homeRuns } returns 0
                    every { runsBattedIn } returns 0
                    every { runs } returns 0
                    every { totalWalks } returns 0
                    every { hitByPitch } returns 0
                    every { sacrificeFlies } returns 0
                    every { totalBases } returns 0
                }

            every { teamRepositoryPort.findByIdWithLeague(1L) } returns team
            every { gameTeamRepositoryPort.findAllByTeamId(1L) } returns listOf(gameTeam)
            every {
                battingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, listOf(1L))
            } returns listOf(battingRecord)
            every { pitchingRecordRepositoryPort.findAllByTeamIdAndGameIds(1L, listOf(1L)) } returns emptyList()

            // when
            val result = teamStatsService.getTeamStats(1L, null, null)

            // then
            assertThat(result.batting.teamBattingAverage).isEqualTo(BigDecimal("0.000"))
            assertThat(result.batting.teamOnBasePercentage).isEqualTo(BigDecimal("0.000"))
            assertThat(result.batting.teamSluggingPercentage).isEqualTo(BigDecimal("0.000"))
        }
    }
}
