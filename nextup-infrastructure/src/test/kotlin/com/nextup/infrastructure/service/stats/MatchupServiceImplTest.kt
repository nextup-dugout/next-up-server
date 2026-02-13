package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("MatchupServiceImpl 테스트")
class MatchupServiceImplTest {
    private lateinit var gameEventRepositoryPort: GameEventRepositoryPort
    private lateinit var playerRepositoryPort: PlayerRepositoryPort
    private lateinit var matchupService: MatchupServiceImpl

    private lateinit var pitcher: Player
    private lateinit var batter: Player

    @BeforeEach
    fun setUp() {
        gameEventRepositoryPort = mockk()
        playerRepositoryPort = mockk()
        matchupService = MatchupServiceImpl(gameEventRepositoryPort, playerRepositoryPort)

        pitcher =
            Player(
                name = "김투수",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = Position.STARTING_PITCHER,
            )
        batter =
            Player(
                name = "이타자",
                birthDate = LocalDate.of(1996, 5, 15),
                primaryPosition = Position.SHORTSTOP,
            )
    }

    @Nested
    @DisplayName("getMatchup")
    inner class GetMatchup {
        @Test
        fun `투수가 존재하지 않으면 예외를 발생시킨다`() {
            // given
            every { playerRepositoryPort.findByIdOrNull(1L) } returns null

            // when & then
            assertThatThrownBy { matchupService.getMatchup(1L, 2L, null, null) }
                .isInstanceOf(PlayerNotFoundException::class.java)
        }

        @Test
        fun `타자가 존재하지 않으면 예외를 발생시킨다`() {
            // given
            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns null

            // when & then
            assertThatThrownBy { matchupService.getMatchup(1L, 2L, null, null) }
                .isInstanceOf(PlayerNotFoundException::class.java)
        }

        @Test
        fun `매치업 기록이 없으면 빈 통계를 반환한다`() {
            // given
            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every { gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(1L, 2L) } returns emptyList()

            // when
            val result = matchupService.getMatchup(1L, 2L, null, null)

            // then
            assertThat(result.pitcherId).isEqualTo(1L)
            assertThat(result.pitcherName).isEqualTo("김투수")
            assertThat(result.batterId).isEqualTo(2L)
            assertThat(result.batterName).isEqualTo("이타자")
            assertThat(result.stats.plateAppearances).isEqualTo(0)
            assertThat(result.stats.atBats).isEqualTo(0)
            assertThat(result.history).isEmpty()
        }

        @Test
        fun `연도 필터를 적용하여 매치업을 조회할 수 있다`() {
            // given
            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every {
                gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatterAndYear(1L, 2L, 2025)
            } returns emptyList()

            // when
            val result = matchupService.getMatchup(1L, 2L, 2025, null)

            // then
            assertThat(result.year).isEqualTo(2025)
            assertThat(result.stats.plateAppearances).isEqualTo(0)
        }

        @Test
        fun `안타 기록을 집계하여 타율을 계산한다`() {
            // given
            val game = mockk<Game>(relaxed = true)
            every { game.id } returns 1L
            every { game.scheduledAt } returns LocalDateTime.of(2025, 4, 15, 14, 0)

            val event1 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.SINGLE
                    every { rbis } returns 0
                    every { description } returns "좌전 안타"
                }
            val event2 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.STRIKEOUT
                    every { rbis } returns 0
                    every { description } returns "삼진"
                }
            val event3 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.SINGLE
                    every { rbis } returns 1
                    every { description } returns "적시타"
                }

            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every {
                gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(1L, 2L)
            } returns listOf(event1, event2, event3)

            // when
            val result = matchupService.getMatchup(1L, 2L, null, null)

            // then
            assertThat(result.stats.plateAppearances).isEqualTo(3)
            assertThat(result.stats.atBats).isEqualTo(3)
            assertThat(result.stats.hits).isEqualTo(2)
            assertThat(result.stats.strikeouts).isEqualTo(1)
            assertThat(result.stats.runsBattedIn).isEqualTo(1)
            assertThat(result.stats.battingAverage).isEqualTo(BigDecimal("0.667"))
        }

        @Test
        fun `2루타, 3루타, 홈런을 집계한다`() {
            // given
            val game = mockk<Game>(relaxed = true)
            every { game.id } returns 1L
            every { game.scheduledAt } returns LocalDateTime.of(2025, 4, 15, 14, 0)

            val event1 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.DOUBLE
                    every { rbis } returns 1
                    every { description } returns "2루타"
                }
            val event2 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.TRIPLE
                    every { rbis } returns 2
                    every { description } returns "3루타"
                }
            val event3 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.HOME_RUN
                    every { rbis } returns 3
                    every { description } returns "홈런"
                }

            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every {
                gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(1L, 2L)
            } returns listOf(event1, event2, event3)

            // when
            val result = matchupService.getMatchup(1L, 2L, null, null)

            // then
            assertThat(result.stats.hits).isEqualTo(3)
            assertThat(result.stats.doubles).isEqualTo(1)
            assertThat(result.stats.triples).isEqualTo(1)
            assertThat(result.stats.homeRuns).isEqualTo(1)
            assertThat(result.stats.runsBattedIn).isEqualTo(6)
            // 총 루타: 2 + 3 + 4 = 9, 타수: 3
            assertThat(result.stats.sluggingPercentage).isEqualTo(BigDecimal("3.000"))
        }

        @Test
        fun `볼넷과 사구를 집계한다`() {
            // given
            val game = mockk<Game>(relaxed = true)
            every { game.id } returns 1L
            every { game.scheduledAt } returns LocalDateTime.of(2025, 4, 15, 14, 0)

            val event1 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.WALK
                    every { rbis } returns 0
                    every { description } returns "볼넷"
                }
            val event2 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.HIT_BY_PITCH
                    every { rbis } returns 0
                    every { description } returns "사구"
                }
            val event3 =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.INTENTIONAL_WALK
                    every { rbis } returns 0
                    every { description } returns "고의사구"
                }

            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every {
                gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(1L, 2L)
            } returns listOf(event1, event2, event3)

            // when
            val result = matchupService.getMatchup(1L, 2L, null, null)

            // then
            assertThat(result.stats.plateAppearances).isEqualTo(3)
            assertThat(result.stats.atBats).isEqualTo(0)
            assertThat(result.stats.walks).isEqualTo(2)
            assertThat(result.stats.hitByPitch).isEqualTo(1)
            assertThat(result.stats.battingAverage).isEqualTo(BigDecimal.ZERO)
            // 출루율: (0 + 2 + 1) / (0 + 2 + 1 + 0) = 1.000
            assertThat(result.stats.onBasePercentage).isEqualTo(BigDecimal("1.000"))
        }

        @Test
        fun `희생플라이를 집계한다`() {
            // given
            val game = mockk<Game>(relaxed = true)
            every { game.id } returns 1L
            every { game.scheduledAt } returns LocalDateTime.of(2025, 4, 15, 14, 0)

            val event =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.SACRIFICE_FLY
                    every { rbis } returns 1
                    every { description } returns "희생플라이"
                }

            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every { gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(1L, 2L) } returns listOf(event)

            // when
            val result = matchupService.getMatchup(1L, 2L, null, null)

            // then
            assertThat(result.stats.plateAppearances).isEqualTo(1)
            assertThat(result.stats.atBats).isEqualTo(0)
            assertThat(result.stats.sacrificeFlies).isEqualTo(1)
            assertThat(result.stats.runsBattedIn).isEqualTo(1)
        }

        @Test
        fun `히스토리를 생성한다`() {
            // given
            val game = mockk<Game>(relaxed = true)
            every { game.id } returns 100L
            every { game.scheduledAt } returns LocalDateTime.of(2025, 4, 15, 14, 0)

            val event =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns PlateAppearanceResult.SINGLE
                    every { rbis } returns 0
                    every { description } returns "좌전 안타"
                }

            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every { gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(1L, 2L) } returns listOf(event)

            // when
            val result = matchupService.getMatchup(1L, 2L, null, null)

            // then
            assertThat(result.history).hasSize(1)
            assertThat(result.history[0].gameId).isEqualTo(100L)
            assertThat(result.history[0].gameDate).isEqualTo("2025-04-15")
            assertThat(result.history[0].result).isEqualTo("1루타")
            assertThat(result.history[0].description).isEqualTo("좌전 안타")
        }

        @Test
        fun `타석 결과가 null인 이벤트는 건너뛴다`() {
            // given
            val game = mockk<Game>(relaxed = true)
            every { game.id } returns 1L
            every { game.scheduledAt } returns LocalDateTime.of(2025, 4, 15, 14, 0)

            val event =
                mockk<GameEvent>(relaxed = true).apply {
                    every { this@apply.game } returns game
                    every { plateAppearanceResult } returns null
                    every { rbis } returns 0
                    every { description } returns "투구"
                }

            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every { gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(1L, 2L) } returns listOf(event)

            // when
            val result = matchupService.getMatchup(1L, 2L, null, null)

            // then
            assertThat(result.stats.plateAppearances).isEqualTo(0)
            assertThat(result.history).hasSize(1)
            assertThat(result.history[0].result).isEqualTo("기록 없음")
        }

        @Test
        fun `출루율의 분모가 0이면 0을 반환한다`() {
            // given
            every { playerRepositoryPort.findByIdOrNull(1L) } returns pitcher
            every { playerRepositoryPort.findByIdOrNull(2L) } returns batter
            every { gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(1L, 2L) } returns emptyList()

            // when
            val result = matchupService.getMatchup(1L, 2L, null, null)

            // then
            assertThat(result.stats.onBasePercentage).isEqualTo(BigDecimal.ZERO)
            assertThat(result.stats.sluggingPercentage).isEqualTo(BigDecimal.ZERO)
        }
    }
}
