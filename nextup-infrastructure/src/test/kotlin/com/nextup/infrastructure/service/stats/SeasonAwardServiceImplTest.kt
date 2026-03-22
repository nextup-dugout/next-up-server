package com.nextup.infrastructure.service.stats

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.stats.SeasonAward
import com.nextup.core.domain.stats.SeasonAwardTitle
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.SeasonAwardRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("SeasonAwardServiceImpl 테스트")
class SeasonAwardServiceImplTest {
    private lateinit var seasonAwardRepository: SeasonAwardRepositoryPort
    private lateinit var seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort
    private lateinit var seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var service: SeasonAwardServiceImpl

    private lateinit var player1: Player
    private lateinit var player2: Player

    @BeforeEach
    fun setUp() {
        seasonAwardRepository = mockk()
        seasonBattingStatsRepository = mockk()
        seasonPitchingStatsRepository = mockk()
        competitionRepository = mockk()
        gameRepository = mockk()

        service =
            SeasonAwardServiceImpl(
                seasonAwardRepository,
                seasonBattingStatsRepository,
                seasonPitchingStatsRepository,
                competitionRepository,
                gameRepository,
            )

        player1 = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP)
        setId(player1, Player::class.java, 10L)

        player2 = Player(name = "김철수", primaryPosition = Position.STARTING_PITCHER)
        setId(player2, Player::class.java, 20L)
    }

    @Nested
    @DisplayName("calculateAndAwardTitles")
    inner class CalculateAndAwardTitles {
        @Test
        fun `연도가 0 이하이면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                service.calculateAndAwardTitles(0, 50, 30)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("연도는 양수여야 합니다")
        }

        @Test
        fun `규정타석이 음수이면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                service.calculateAndAwardTitles(2026, -1, 30)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("규정타석은 0 이상이어야 합니다")
        }

        @Test
        fun `규정이닝이 음수이면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                service.calculateAndAwardTitles(2026, 50, -1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("규정이닝은 0 이상이어야 합니다")
        }

        @Test
        fun `모든 부문에서 리더가 있을 때 모든 타이틀이 부여된다`() {
            // given
            val battingStats1 =
                createBattingStats(player1, 2026, atBats = 100, hits = 35, pa = 110)
            val battingStats2 =
                createBattingStats(player2, 2026, homeRuns = 10, rbi = 30, stolenBases = 15, hits = 40)

            val pitchingStats1 =
                createPitchingStats(player2, 2026, wins = 8, ipOuts = 54, earnedRuns = 6, saves = 5, strikeouts = 50)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }

            // 타격 부문 mock
            every {
                seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1)
            } returns listOf(battingStats1)
            every {
                seasonBattingStatsRepository.findTopByHomeRuns(2026, 1)
            } returns listOf(battingStats2)
            every {
                seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1)
            } returns listOf(battingStats2)
            every {
                seasonBattingStatsRepository.findTopByStolenBases(2026, 1)
            } returns listOf(battingStats2)
            every {
                seasonBattingStatsRepository.findTopByHits(2026, 1)
            } returns listOf(battingStats2)

            // 투수 부문 mock
            every {
                seasonPitchingStatsRepository.findTopByWins(2026, 1)
            } returns listOf(pitchingStats1)
            every {
                seasonPitchingStatsRepository.findTopByEra(2026, 30, 1)
            } returns listOf(pitchingStats1)
            every {
                seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1)
            } returns listOf(pitchingStats1)
            every {
                seasonPitchingStatsRepository.findTopBySaves(2026, 1)
            } returns listOf(pitchingStats1)

            val savedSlot = slot<List<SeasonAward>>()
            every { seasonAwardRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).hasSize(9) // 5 batting + 4 pitching
            verify(exactly = 1) { seasonAwardRepository.deleteAllByYear(2026) }
            verify(exactly = 1) { seasonAwardRepository.saveAll(any()) }
        }

        @Test
        fun `모든 부문에서 리더가 없을 때 빈 목록이 반환된다`() {
            // given
            justRun { seasonAwardRepository.deleteAllByYear(2026) }

            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()

            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `홈런이 0인 리더는 홈런왕이 부여되지 않는다`() {
            // given
            val battingStats =
                createBattingStats(player1, 2026, homeRuns = 0)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns listOf(battingStats)
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `타점이 0인 리더는 타점왕이 부여되지 않는다`() {
            // given
            val battingStats =
                createBattingStats(player1, 2026, rbi = 0)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns listOf(battingStats)
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `도루가 0인 리더는 도루왕이 부여되지 않는다`() {
            // given
            val battingStats =
                createBattingStats(player1, 2026, stolenBases = 0)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns listOf(battingStats)
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `안타가 0인 리더는 최다안타가 부여되지 않는다`() {
            // given
            val battingStats =
                createBattingStats(player1, 2026, hits = 0)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns listOf(battingStats)
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `승수가 0인 리더는 다승왕이 부여되지 않는다`() {
            // given
            val pitchingStats =
                createPitchingStats(player2, 2026, wins = 0)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns listOf(pitchingStats)
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `삼진이 0인 리더는 탈삼진왕이 부여되지 않는다`() {
            // given
            val pitchingStats =
                createPitchingStats(player2, 2026, strikeouts = 0)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns listOf(pitchingStats)
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `세이브가 0인 리더는 세이브왕이 부여되지 않는다`() {
            // given
            val pitchingStats =
                createPitchingStats(player2, 2026, saves = 0)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns listOf(pitchingStats)

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `ERA 리더의 earnedRunAverage가 null이면 방어율 타이틀이 부여되지 않는다`() {
            // given: 0이닝이지만 자책점이 있는 경우 ERA는 null
            val pitchingStats =
                createPitchingStats(player2, 2026, ipOuts = 0, earnedRuns = 1)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns listOf(pitchingStats)
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `타격왕의 statValue가 타율로 설정된다`() {
            // given
            val battingStats =
                createBattingStats(player1, 2026, atBats = 100, hits = 35, pa = 110)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns listOf(battingStats)
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            val savedSlot = slot<List<SeasonAward>>()
            every { seasonAwardRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(SeasonAwardTitle.BATTING_CHAMPION)
            assertThat(result[0].player).isEqualTo(player1)
            assertThat(result[0].statValue).isEqualTo(battingStats.battingAverage)
        }

        @Test
        fun `방어율 타이틀의 statValue가 ERA로 설정된다`() {
            // given
            val pitchingStats =
                createPitchingStats(player2, 2026, ipOuts = 54, earnedRuns = 6)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns listOf(pitchingStats)
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            val savedSlot = slot<List<SeasonAward>>()
            every { seasonAwardRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(SeasonAwardTitle.ERA_TITLE)
            assertThat(result[0].statValue).isEqualTo(pitchingStats.earnedRunAverage)
        }

        @Test
        fun `기존 타이틀이 삭제된 후 재계산된다`() {
            // given
            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()
            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            service.calculateAndAwardTitles(2026, 50, 30)

            // then
            verify(exactly = 1) { seasonAwardRepository.deleteAllByYear(2026) }
        }

        @Test
        fun `타격 부문만 리더가 있을 때 타격 타이틀만 부여된다`() {
            // given
            val battingStats =
                createBattingStats(player1, 2026, homeRuns = 15)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns listOf(battingStats)
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            val savedSlot = slot<List<SeasonAward>>()
            every { seasonAwardRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(SeasonAwardTitle.HOME_RUN_KING)
            assertThat(result[0].statValue).isEqualByComparingTo(BigDecimal(15))
        }

        @Test
        fun `투수 부문만 리더가 있을 때 투수 타이틀만 부여된다`() {
            // given
            val pitchingStats =
                createPitchingStats(player2, 2026, wins = 10)

            justRun { seasonAwardRepository.deleteAllByYear(2026) }
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 50, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns listOf(pitchingStats)
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            val savedSlot = slot<List<SeasonAward>>()
            every { seasonAwardRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            val result = service.calculateAndAwardTitles(2026, 50, 30)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(SeasonAwardTitle.WINS_LEADER)
        }
    }

    @Nested
    @DisplayName("getAwardsByYear")
    inner class GetAwardsByYear {
        @Test
        fun `연도별 타이틀 목록을 반환한다`() {
            // given
            val award1 =
                SeasonAward.create(
                    player = player1,
                    year = 2026,
                    title = SeasonAwardTitle.BATTING_CHAMPION,
                    statValue = BigDecimal("0.350"),
                )
            val award2 =
                SeasonAward.create(
                    player = player2,
                    year = 2026,
                    title = SeasonAwardTitle.ERA_TITLE,
                    statValue = BigDecimal("2.50"),
                )

            every { seasonAwardRepository.findAllByYear(2026) } returns listOf(award1, award2)

            // when
            val result = service.getAwardsByYear(2026)

            // then
            assertThat(result).hasSize(2)
            verify(exactly = 1) { seasonAwardRepository.findAllByYear(2026) }
        }

        @Test
        fun `해당 연도에 타이틀이 없으면 빈 목록을 반환한다`() {
            // given
            every { seasonAwardRepository.findAllByYear(2025) } returns emptyList()

            // when
            val result = service.getAwardsByYear(2025)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getAwardsByPlayerId")
    inner class GetAwardsByPlayerId {
        @Test
        fun `선수별 타이틀 목록을 반환한다`() {
            // given
            val award =
                SeasonAward.create(
                    player = player1,
                    year = 2026,
                    title = SeasonAwardTitle.BATTING_CHAMPION,
                    statValue = BigDecimal("0.350"),
                )

            every { seasonAwardRepository.findAllByPlayerId(10L) } returns listOf(award)

            // when
            val result = service.getAwardsByPlayerId(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].player).isEqualTo(player1)
            verify(exactly = 1) { seasonAwardRepository.findAllByPlayerId(10L) }
        }

        @Test
        fun `해당 선수에 타이틀이 없으면 빈 목록을 반환한다`() {
            // given
            every { seasonAwardRepository.findAllByPlayerId(999L) } returns emptyList()

            // when
            val result = service.getAwardsByPlayerId(999L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getAwardsByCompetitionId")
    inner class GetAwardsByCompetitionId {
        @Test
        fun `대회별 타이틀 목록을 반환한다`() {
            // given
            val award =
                SeasonAward.create(
                    player = player1,
                    year = 2026,
                    title = SeasonAwardTitle.BATTING_CHAMPION,
                    statValue = BigDecimal("0.350"),
                    competitionId = 1L,
                )

            every { seasonAwardRepository.findAllByCompetitionId(1L) } returns listOf(award)

            // when
            val result = service.getAwardsByCompetitionId(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].competitionId).isEqualTo(1L)
            verify(exactly = 1) { seasonAwardRepository.findAllByCompetitionId(1L) }
        }

        @Test
        fun `해당 대회에 타이틀이 없으면 빈 목록을 반환한다`() {
            // given
            every { seasonAwardRepository.findAllByCompetitionId(999L) } returns emptyList()

            // when
            val result = service.getAwardsByCompetitionId(999L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("calculateAndAwardTitlesByCompetition")
    inner class CalculateAndAwardTitlesByCompetition {
        @Test
        fun `존재하지 않는 대회이면 예외가 발생한다`() {
            // given
            every { competitionRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.calculateAndAwardTitlesByCompetition(999L)
            }.isInstanceOf(com.nextup.common.exception.CompetitionNotFoundException::class.java)
        }

        @Test
        fun `대회 기반 타이틀 계산 — 경기가 있을 때 모든 타이틀이 부여된다`() {
            // given
            val gameRules = GameRules(qualificationPAMultiplier = 3.1, qualificationIPMultiplier = 1.0)
            val competition = mockk<Competition>()
            every { competition.year } returns 2026
            every { competition.gameRules } returns gameRules

            every { competitionRepository.findByIdOrNull(1L) } returns competition

            // 2개 팀, 각 팀 5경기씩 출전
            val team1 = mockk<Team>()
            every { team1.id } returns 100L
            val team2 = mockk<Team>()
            every { team2.id } returns 200L

            val games = (1..5).map { _ ->
                val game = mockk<Game>()
                val gt1 = mockk<GameTeam>()
                every { gt1.team } returns team1
                val gt2 = mockk<GameTeam>()
                every { gt2.team } returns team2
                every { game.gameTeams } returns listOf(gt1, gt2)
                game
            }
            every { gameRepository.findByCompetitionId(1L) } returns games

            // 팀당 5경기 → 규정타석 = round(5 * 3.1) = 16, 규정이닝아웃 = round(5 * 1.0 * 3) = 15
            justRun { seasonAwardRepository.deleteAllByCompetitionId(1L) }

            val battingStats1 =
                createBattingStats(player1, 2026, atBats = 100, hits = 35, pa = 110)
            val battingStats2 =
                createBattingStats(player2, 2026, homeRuns = 10, rbi = 30, stolenBases = 15, hits = 40)
            val pitchingStats1 =
                createPitchingStats(player2, 2026, wins = 8, ipOuts = 54, earnedRuns = 6, saves = 5, strikeouts = 50)

            every {
                seasonBattingStatsRepository.findTopByBattingAverage(2026, 16, 1)
            } returns listOf(battingStats1)
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns listOf(battingStats2)
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns listOf(battingStats2)
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns listOf(battingStats2)
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns listOf(battingStats2)

            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns listOf(pitchingStats1)
            every { seasonPitchingStatsRepository.findTopByEra(2026, 15, 1) } returns listOf(pitchingStats1)
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns listOf(pitchingStats1)
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns listOf(pitchingStats1)

            val savedSlot = slot<List<SeasonAward>>()
            every { seasonAwardRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            val result = service.calculateAndAwardTitlesByCompetition(1L)

            // then
            assertThat(result).hasSize(9) // 5 batting + 4 pitching
            assertThat(result).allSatisfy { award ->
                assertThat(award.competitionId).isEqualTo(1L)
            }
            verify(exactly = 1) { seasonAwardRepository.deleteAllByCompetitionId(1L) }
            verify(exactly = 1) { seasonAwardRepository.saveAll(any()) }
        }

        @Test
        fun `대회 기반 타이틀 계산 — 경기가 없으면 규정타석과 규정이닝이 0이다`() {
            // given
            val gameRules = GameRules(qualificationPAMultiplier = 3.1, qualificationIPMultiplier = 1.0)
            val competition = mockk<Competition>()
            every { competition.year } returns 2026
            every { competition.gameRules } returns gameRules

            every { competitionRepository.findByIdOrNull(2L) } returns competition
            every { gameRepository.findByCompetitionId(2L) } returns emptyList()

            justRun { seasonAwardRepository.deleteAllByCompetitionId(2L) }

            // gamesPerTeam=0 → minPA=0, minIPOuts=0
            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 0, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 0, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitlesByCompetition(2L)

            // then
            assertThat(result).isEmpty()
            verify(exactly = 1) { seasonAwardRepository.deleteAllByCompetitionId(2L) }
        }

        @Test
        fun `대회 기반 타이틀 계산 — 팀별 경기 수 평균이 올바르게 계산된다`() {
            // given: 3팀, 팀A 4경기, 팀B 4경기, 팀C 2경기 → 평균 round(3.33)=3
            val gameRules = GameRules(qualificationPAMultiplier = 3.1, qualificationIPMultiplier = 1.0)
            val competition = mockk<Competition>()
            every { competition.year } returns 2026
            every { competition.gameRules } returns gameRules

            every { competitionRepository.findByIdOrNull(3L) } returns competition

            val teamA = mockk<Team>()
            every { teamA.id } returns 100L
            val teamB = mockk<Team>()
            every { teamB.id } returns 200L
            val teamC = mockk<Team>()
            every { teamC.id } returns 300L

            // 4 games: A vs B (2 games), A vs C (1 game), B vs C (1 game)
            // teamA: 3 games, teamB: 3 games, teamC: 2 games → avg = round(2.67) = 3
            // 하지만 round to int 방식 차이 고려. 직접 계산: (3+3+2)/3 = 2.667 → roundToInt=3
            fun makeGame(t1: Team, t2: Team): Game {
                val game = mockk<Game>()
                val gt1 = mockk<GameTeam>()
                every { gt1.team } returns t1
                val gt2 = mockk<GameTeam>()
                every { gt2.team } returns t2
                every { game.gameTeams } returns listOf(gt1, gt2)
                return game
            }

            val games =
                listOf(
                    makeGame(teamA, teamB),
                    makeGame(teamA, teamB),
                    makeGame(teamA, teamC),
                    makeGame(teamB, teamC),
                )
            every { gameRepository.findByCompetitionId(3L) } returns games

            // 팀당 경기수 3 → 규정타석 = round(3 * 3.1) = 9, 규정이닝아웃 = round(3 * 1.0 * 3) = 9
            justRun { seasonAwardRepository.deleteAllByCompetitionId(3L) }

            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 9, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 9, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            every { seasonAwardRepository.saveAll(any()) } returns emptyList()

            // when
            val result = service.calculateAndAwardTitlesByCompetition(3L)

            // then
            assertThat(result).isEmpty()
            verify(exactly = 1) { seasonAwardRepository.deleteAllByCompetitionId(3L) }
        }

        @Test
        fun `대회 기반 타이틀에 competitionId가 올바르게 설정된다`() {
            // given
            val gameRules = GameRules(qualificationPAMultiplier = 3.1, qualificationIPMultiplier = 1.0)
            val competition = mockk<Competition>()
            every { competition.year } returns 2026
            every { competition.gameRules } returns gameRules

            every { competitionRepository.findByIdOrNull(5L) } returns competition

            val team1 = mockk<Team>()
            every { team1.id } returns 100L
            val team2 = mockk<Team>()
            every { team2.id } returns 200L

            // 2 games
            val games = (1..2).map {
                val game = mockk<Game>()
                val gt1 = mockk<GameTeam>()
                every { gt1.team } returns team1
                val gt2 = mockk<GameTeam>()
                every { gt2.team } returns team2
                every { game.gameTeams } returns listOf(gt1, gt2)
                game
            }
            every { gameRepository.findByCompetitionId(5L) } returns games

            // 팀당 2경기 → 규정타석 = round(2 * 3.1) = 6, 규정이닝아웃 = round(2 * 1.0 * 3) = 6
            justRun { seasonAwardRepository.deleteAllByCompetitionId(5L) }

            val battingStats =
                createBattingStats(player1, 2026, atBats = 100, hits = 35, pa = 110)

            every { seasonBattingStatsRepository.findTopByBattingAverage(2026, 6, 1) } returns listOf(battingStats)
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns emptyList()
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByEra(2026, 6, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns emptyList()
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns emptyList()

            val savedSlot = slot<List<SeasonAward>>()
            every { seasonAwardRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            val result = service.calculateAndAwardTitlesByCompetition(5L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(SeasonAwardTitle.BATTING_CHAMPION)
            assertThat(result[0].competitionId).isEqualTo(5L)
            assertThat(result[0].year).isEqualTo(2026)
            assertThat(result[0].player).isEqualTo(player1)
        }

        @Test
        fun `대회 기반 타이틀 계산 — 타격과 투수 모든 부문 리더 존재 시 9개 타이틀이 부여된다`() {
            // given
            val gameRules = GameRules(qualificationPAMultiplier = 3.1, qualificationIPMultiplier = 1.0)
            val competition = mockk<Competition>()
            every { competition.year } returns 2026
            every { competition.gameRules } returns gameRules

            every { competitionRepository.findByIdOrNull(10L) } returns competition

            val team1 = mockk<Team>()
            every { team1.id } returns 100L
            val team2 = mockk<Team>()
            every { team2.id } returns 200L

            // 10 games per team
            val games = (1..10).map {
                val game = mockk<Game>()
                val gt1 = mockk<GameTeam>()
                every { gt1.team } returns team1
                val gt2 = mockk<GameTeam>()
                every { gt2.team } returns team2
                every { game.gameTeams } returns listOf(gt1, gt2)
                game
            }
            every { gameRepository.findByCompetitionId(10L) } returns games

            // 팀당 10경기 → 규정타석 = round(10 * 3.1) = 31, 규정이닝아웃 = round(10 * 1.0 * 3) = 30
            justRun { seasonAwardRepository.deleteAllByCompetitionId(10L) }

            val battingStats1 =
                createBattingStats(player1, 2026, atBats = 100, hits = 35, pa = 110)
            val battingStats2 =
                createBattingStats(player2, 2026, homeRuns = 10, rbi = 30, stolenBases = 15, hits = 40)
            val pitchingStats1 =
                createPitchingStats(player2, 2026, wins = 8, ipOuts = 54, earnedRuns = 6, saves = 5, strikeouts = 50)

            every {
                seasonBattingStatsRepository.findTopByBattingAverage(2026, 31, 1)
            } returns listOf(battingStats1)
            every { seasonBattingStatsRepository.findTopByHomeRuns(2026, 1) } returns listOf(battingStats2)
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(2026, 1) } returns listOf(battingStats2)
            every { seasonBattingStatsRepository.findTopByStolenBases(2026, 1) } returns listOf(battingStats2)
            every { seasonBattingStatsRepository.findTopByHits(2026, 1) } returns listOf(battingStats2)

            every { seasonPitchingStatsRepository.findTopByWins(2026, 1) } returns listOf(pitchingStats1)
            every { seasonPitchingStatsRepository.findTopByEra(2026, 30, 1) } returns listOf(pitchingStats1)
            every { seasonPitchingStatsRepository.findTopByStrikeouts(2026, 1) } returns listOf(pitchingStats1)
            every { seasonPitchingStatsRepository.findTopBySaves(2026, 1) } returns listOf(pitchingStats1)

            val savedSlot = slot<List<SeasonAward>>()
            every { seasonAwardRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            val result = service.calculateAndAwardTitlesByCompetition(10L)

            // then
            assertThat(result).hasSize(9)
            val titles = result.map { it.title }
            assertThat(titles).containsExactlyInAnyOrder(
                SeasonAwardTitle.BATTING_CHAMPION,
                SeasonAwardTitle.HOME_RUN_KING,
                SeasonAwardTitle.RBI_KING,
                SeasonAwardTitle.STOLEN_BASE_KING,
                SeasonAwardTitle.HITS_LEADER,
                SeasonAwardTitle.WINS_LEADER,
                SeasonAwardTitle.ERA_TITLE,
                SeasonAwardTitle.STRIKEOUT_KING,
                SeasonAwardTitle.SAVES_LEADER,
            )
            // 모든 타이틀에 competitionId가 설정됨
            assertThat(result).allSatisfy { award ->
                assertThat(award.competitionId).isEqualTo(10L)
            }
        }
    }

    // Helper methods

    private fun createBattingStats(
        player: Player,
        year: Int,
        atBats: Int = 0,
        hits: Int = 0,
        pa: Int = 0,
        homeRuns: Int = 0,
        rbi: Int = 0,
        stolenBases: Int = 0,
    ): SeasonBattingStats {
        val stats = SeasonBattingStats(player = player, year = year)
        setField(stats, "atBats", atBats)
        setField(stats, "hits", hits)
        setField(stats, "plateAppearances", pa)
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
        runsAllowed: Int = 0,
        wins: Int = 0,
        saves: Int = 0,
        strikeouts: Int = 0,
    ): SeasonPitchingStats {
        val stats = SeasonPitchingStats(player = player, year = year)
        setField(stats, "inningsPitchedOuts", ipOuts)
        setField(stats, "earnedRuns", earnedRuns)
        setField(stats, "runsAllowed", maxOf(runsAllowed, earnedRuns))
        setField(stats, "wins", wins)
        setField(stats, "saves", saves)
        setField(stats, "strikeouts", strikeouts)
        return stats
    }

    private fun setField(
        target: Any,
        fieldName: String,
        value: Any,
    ) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun <T> setId(
        target: T,
        clazz: Class<T>,
        id: Long,
    ) {
        val idField = clazz.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(target, id)
    }
}
