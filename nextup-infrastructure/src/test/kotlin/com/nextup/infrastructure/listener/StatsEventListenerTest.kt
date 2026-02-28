package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("StatsEventListener 테스트")
class StatsEventListenerTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()

    private val listener = StatsEventListener(seasonBattingStatsRepository, gameRepository)

    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    private val mockGame = mockk<Game>()

    @BeforeEach
    fun setUp() {
        every { mockGame.scheduledAt } returns LocalDateTime.of(2024, 5, 15, 18, 0)
        every { gameRepository.findByIdOrNull(any()) } returns mockGame
    }

    @Nested
    @DisplayName("onPlateAppearanceRecorded - 타석 결과 기록 이벤트 처리")
    inner class OnPlateAppearanceRecorded {
        @Test
        fun `타석 결과가 시즌 통계에 즉시 반영됨`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(1)
            verify { seasonBattingStatsRepository.save(stats) }
        }

        @Test
        fun `홈런 기록 이벤트 처리 시 홈런 통계 증가`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.HOME_RUN,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(stats.homeRuns).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(1)
        }

        @Test
        fun `볼넷 기록 이벤트 처리 시 볼넷 통계 증가 (타수 제외)`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.WALK,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isZero
            assertThat(stats.walks).isEqualTo(1)
        }

        @Test
        fun `시즌 통계가 없는 경우 저장 없이 건너뜀`() {
            // given
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            verify(exactly = 0) { seasonBattingStatsRepository.save(any()) }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 999L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onPlateAppearanceRecorded(event)
            }
        }

        @Test
        fun `경기 년도에 맞는 시즌 통계를 올바르게 조회함`() {
            // given: 2023년 경기
            every { mockGame.scheduledAt } returns LocalDateTime.of(2023, 9, 1, 14, 0)
            val stats = SeasonBattingStats.create(testPlayer, 2023)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2023)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.DOUBLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            verify { seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2023) }
            assertThat(stats.doubles).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("onPlateAppearanceUndone - 타석 결과 취소 이벤트 처리")
    inner class OnPlateAppearanceUndone {
        @Test
        fun `타석 결과가 시즌 통계에서 역산됨`() {
            // given: 이미 단타가 반영된 통계
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
            verify { seasonBattingStatsRepository.save(stats) }
        }

        @Test
        fun `홈런 Undo 이벤트 처리 시 홈런 통계 감소`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)

            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.HOME_RUN,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then
            assertThat(stats.homeRuns).isZero
            assertThat(stats.hits).isZero
        }

        @Test
        fun `시즌 통계가 없는 경우 저장 없이 건너뜀`() {
            // given
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then
            verify(exactly = 0) { seasonBattingStatsRepository.save(any()) }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 999L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onPlateAppearanceUndone(event)
            }
        }
    }
}
