package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.time.LocalDateTime

@DisplayName("StatsEventListener 테스트")
class StatsEventListenerTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val seasonPitchingStatsRepository = mockk<SeasonPitchingStatsRepositoryPort>()
    private val seasonFieldingStatsRepository = mockk<SeasonFieldingStatsRepositoryPort>()
    private val careerBattingStatsRepository = mockk<CareerBattingStatsRepositoryPort>()
    private val careerPitchingStatsRepository = mockk<CareerPitchingStatsRepositoryPort>()
    private val careerFieldingStatsRepository = mockk<CareerFieldingStatsRepositoryPort>()
    private val battingRecordRepository = mockk<BattingRecordRepositoryPort>()
    private val pitchingRecordRepository = mockk<PitchingRecordRepositoryPort>()
    private val fieldingRecordRepository = mockk<FieldingRecordRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val playerRepository = mockk<PlayerRepositoryPort>()

    private val listener =
        StatsEventListener(
            seasonBattingStatsRepository = seasonBattingStatsRepository,
            seasonPitchingStatsRepository = seasonPitchingStatsRepository,
            seasonFieldingStatsRepository = seasonFieldingStatsRepository,
            careerBattingStatsRepository = careerBattingStatsRepository,
            careerPitchingStatsRepository = careerPitchingStatsRepository,
            careerFieldingStatsRepository = careerFieldingStatsRepository,
            battingRecordRepository = battingRecordRepository,
            pitchingRecordRepository = pitchingRecordRepository,
            fieldingRecordRepository = fieldingRecordRepository,
            gameRepository = gameRepository,
            playerRepository = playerRepository,
        )

    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    private val testPitcher =
        Player(
            name = "박투수",
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 2L,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
        fun `시즌 통계가 없는 경우 자동 생성 후 타석 결과 반영`() {
            // given
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then: 자동 생성 후 저장 (생성 1회 + 갱신 1회 = save 2회)
            verify(exactly = 2) { seasonBattingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPlayer.id) }
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `시즌 통계가 없는 신규 선수의 첫 타석 기록 시 통계가 올바르게 반영됨`() {
            // given
            val savedStats = mutableListOf<SeasonBattingStats>()
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { seasonBattingStatsRepository.save(capture(savedStats)) } answers { firstArg() }

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.HOME_RUN,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then: 마지막 저장된 stats에 홈런이 반영되어 있어야 함
            val finalStats = savedStats.last()
            assertThat(finalStats.plateAppearances).isEqualTo(1)
            assertThat(finalStats.atBats).isEqualTo(1)
            assertThat(finalStats.hits).isEqualTo(1)
            assertThat(finalStats.homeRuns).isEqualTo(1)
            assertThat(finalStats.year).isEqualTo(2024)
            assertThat(finalStats.player).isEqualTo(testPlayer)
        }

        @Test
        fun `선수를 찾을 수 없는 경우 PlayerNotFoundException 발생`() {
            // given
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(999L, 2024)
            } returns null
            every { playerRepository.findByIdOrNull(999L) } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 999L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            assertThrows<PlayerNotFoundException> {
                listener.onPlateAppearanceRecorded(event)
            }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 999L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2023)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2023)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2023)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.DOUBLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            verify { seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2023) }
            assertThat(stats.doubles).isEqualTo(1)
        }

        @Test
        fun `타석 결과가 투수 시즌 통계에도 즉시 반영됨`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.hitsAllowed).isEqualTo(1)
            verify { seasonPitchingStatsRepository.save(pitchingStats) }
        }

        @Test
        fun `홈런 기록 시 투수 통계에 피안타와 피홈런 모두 반영됨`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.HOME_RUN,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.hitsAllowed).isEqualTo(1)
            assertThat(pitchingStats.homeRunsAllowed).isEqualTo(1)
        }

        @Test
        fun `삼진 기록 시 투수 삼진 통계 증가`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.STRIKEOUT,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.strikeouts).isEqualTo(1)
        }

        @Test
        fun `볼넷 기록 시 투수 볼넷 통계 증가`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.WALK,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.walksAllowed).isEqualTo(1)
        }

        @Test
        fun `투수 시즌 통계가 없는 경우 투수 통계 갱신 건너뜀`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then: 타격 통계는 저장, 투수 통계는 저장 안 됨
            verify(exactly = 1) { seasonBattingStatsRepository.save(any()) }
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            pitchingStats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            pitchingStats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)

            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.HOME_RUN,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then
            assertThat(stats.homeRuns).isZero
            assertThat(stats.hits).isZero
        }

        @Test
        fun `시즌 통계가 없는 경우 자동 생성 후 역산 처리`() {
            // given
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then: 자동 생성 후 저장 (생성 1회 + 갱신 1회 = save 2회)
            verify(exactly = 2) { seasonBattingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPlayer.id) }
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `Undo 시 투수 통계도 역산됨`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            battingStats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            pitchingStats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then
            assertThat(pitchingStats.battersFaced).isZero
            assertThat(pitchingStats.hitsAllowed).isZero
            verify { seasonPitchingStatsRepository.save(pitchingStats) }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 999L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onPlateAppearanceUndone(event)
            }
        }
    }

    @Nested
    @DisplayName("onGameResultConfirmed - 경기 결과 확정 이벤트 처리")
    inner class OnGameResultConfirmed {
        private val mockBattingRecord = mockk<BattingRecord>(relaxed = true)
        private val mockPitchingRecord = mockk<PitchingRecord>(relaxed = true)
        private val mockFieldingRecord = mockk<FieldingRecord>(relaxed = true)
        private val mockGamePlayer = mockk<GamePlayer>()
        private val mockPitcherGamePlayer = mockk<GamePlayer>()
        private val mockFielderGamePlayer = mockk<GamePlayer>()

        private val gameId = 10L
        private val event =
            GameResultConfirmedEvent(
                gameId = gameId,
                homeTeamId = 1L,
                awayTeamId = 2L,
                homeScore = 3,
                awayScore = 1,
            )

        @BeforeEach
        fun setUpRecords() {
            every { mockBattingRecord.gamePlayer } returns mockGamePlayer
            every { mockGamePlayer.player } returns testPlayer
            every { mockPitchingRecord.gamePlayer } returns mockPitcherGamePlayer
            every { mockPitcherGamePlayer.player } returns testPitcher
            every { mockFieldingRecord.gamePlayer } returns mockFielderGamePlayer
            every { mockFielderGamePlayer.player } returns testPlayer
            // 기본적으로 수비 기록 없음 (수비 테스트에서만 오버라이드)
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns emptyList()
        }

        @Test
        fun `경기 종료 시 SeasonPitchingStats가 신규 생성되고 기록이 누적됨`() {
            // given
            val careerPitching = CareerPitchingStats.create(testPitcher)

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns null
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 새 SeasonPitchingStats 생성 및 저장 확인
            verify(exactly = 1) { seasonPitchingStatsRepository.save(any()) }
            // CareerPitchingStats도 저장됨
            verify(exactly = 1) { careerPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `경기 종료 시 기존 SeasonPitchingStats에 기록이 누적됨`() {
            // given
            val seasonPitching = SeasonPitchingStats.create(testPitcher, 2024)
            val careerPitching = CareerPitchingStats.create(testPitcher)
            careerPitching.addSeason()

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns seasonPitching
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            val initialGames = seasonPitching.gamesPlayed

            // when
            listener.onGameResultConfirmed(event)

            // then: gamesPlayed 가 1 증가함
            assertThat(seasonPitching.gamesPlayed).isEqualTo(initialGames + 1)
            verify(exactly = 1) { seasonPitchingStatsRepository.save(seasonPitching) }
        }

        @Test
        fun `첫 시즌 투수 기록 시 CareerPitchingStats에 addSeason 호출됨`() {
            // given: 시즌 통계 없음(첫 시즌)
            val careerPitching = CareerPitchingStats.create(testPitcher)

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns null
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 첫 시즌이므로 seasonsPlayed = 1
            assertThat(careerPitching.seasonsPlayed).isEqualTo(1)
        }

        @Test
        fun `기존 시즌 투수 기록 시 CareerPitchingStats에 addSeason 호출되지 않음`() {
            // given: 시즌 통계 이미 존재
            val existingSeason = SeasonPitchingStats.create(testPitcher, 2024)
            val careerPitching = CareerPitchingStats.create(testPitcher)
            careerPitching.addSeason() // 이미 시즌 카운트 1

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYear(testPitcher.id, 2024)
            } returns existingSeason
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            val initialSeasons = careerPitching.seasonsPlayed

            // when
            listener.onGameResultConfirmed(event)

            // then: addSeason 호출 안 됨 - seasonsPlayed 변화 없음
            assertThat(careerPitching.seasonsPlayed).isEqualTo(initialSeasons)
        }

        @Test
        fun `경기 종료 시 CareerBattingStats가 신규 생성되고 기록이 누적됨`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every { careerBattingStatsRepository.findByPlayerId(testPlayer.id) } returns null
            every { careerBattingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 새 CareerBattingStats 생성 및 저장 확인
            verify(exactly = 1) { careerBattingStatsRepository.save(any()) }
        }

        @Test
        fun `경기 종료 시 기존 CareerBattingStats에 기록이 누적됨`() {
            // given: 이미 존재하는 커리어 통계
            val careerBatting = CareerBattingStats.create(testPlayer)
            careerBatting.addSeason()
            val existingSeason = SeasonBattingStats.create(testPlayer, 2024)

            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns existingSeason
            every { careerBattingStatsRepository.findByPlayerId(testPlayer.id) } returns careerBatting
            every { careerBattingStatsRepository.save(any()) } answers { firstArg() }

            val initialGames = careerBatting.gamesPlayed

            // when
            listener.onGameResultConfirmed(event)

            // then: gamesPlayed 가 1 증가함
            assertThat(careerBatting.gamesPlayed).isEqualTo(initialGames + 1)
            verify(exactly = 1) { careerBattingStatsRepository.save(careerBatting) }
        }

        @Test
        fun `첫 시즌 타격 기록 시 CareerBattingStats에 addSeason 호출됨`() {
            // given: 시즌 타격 통계 없음 (첫 시즌)
            val careerBatting = CareerBattingStats.create(testPlayer)

            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every { careerBattingStatsRepository.findByPlayerId(testPlayer.id) } returns careerBatting
            every { careerBattingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 첫 시즌이므로 seasonsPlayed = 1
            assertThat(careerBatting.seasonsPlayed).isEqualTo(1)
        }

        @Test
        fun `기존 시즌 타격 기록 시 CareerBattingStats에 addSeason 호출되지 않음`() {
            // given: 이미 시즌 타격 통계 존재
            val existingSeason = SeasonBattingStats.create(testPlayer, 2024)
            val careerBatting = CareerBattingStats.create(testPlayer)
            careerBatting.addSeason()

            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns existingSeason
            every { careerBattingStatsRepository.findByPlayerId(testPlayer.id) } returns careerBatting
            every { careerBattingStatsRepository.save(any()) } answers { firstArg() }

            val initialSeasons = careerBatting.seasonsPlayed

            // when
            listener.onGameResultConfirmed(event)

            // then: addSeason 호출 안 됨
            assertThat(careerBatting.seasonsPlayed).isEqualTo(initialSeasons)
        }

        @Test
        fun `경기 종료 시 SeasonFieldingStats가 신규 생성되고 기록이 누적됨`() {
            // given
            val careerFielding = CareerFieldingStats.create(testPlayer)

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(testPlayer.id) } returns careerFielding
            every { careerFieldingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 새 SeasonFieldingStats 생성 및 저장 확인
            verify(exactly = 1) { seasonFieldingStatsRepository.save(any()) }
            verify(exactly = 1) { careerFieldingStatsRepository.save(any()) }
        }

        @Test
        fun `경기 종료 시 기존 SeasonFieldingStats에 기록이 누적됨`() {
            // given
            val seasonFielding = SeasonFieldingStats.create(testPlayer, 2024)
            val careerFielding = CareerFieldingStats.create(testPlayer)
            careerFielding.addSeason()

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns seasonFielding
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(testPlayer.id) } returns careerFielding
            every { careerFieldingStatsRepository.save(any()) } answers { firstArg() }

            val initialGames = seasonFielding.gamesPlayed

            // when
            listener.onGameResultConfirmed(event)

            // then: gamesPlayed 가 1 증가함
            assertThat(seasonFielding.gamesPlayed).isEqualTo(initialGames + 1)
            verify(exactly = 1) { seasonFieldingStatsRepository.save(seasonFielding) }
        }

        @Test
        fun `첫 시즌 수비 기록 시 CareerFieldingStats에 addSeason 호출됨`() {
            // given: 시즌 통계 없음(첫 시즌)
            val careerFielding = CareerFieldingStats.create(testPlayer)

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(testPlayer.id) } returns careerFielding
            every { careerFieldingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 첫 시즌이므로 seasonsPlayed = 1
            assertThat(careerFielding.seasonsPlayed).isEqualTo(1)
        }

        @Test
        fun `CareerFieldingStats가 없으면 신규 생성됨`() {
            // given: careerFieldingStatsRepository.findByPlayerId returns null
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(testPlayer.id) } returns null
            every { careerFieldingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 새 CareerFieldingStats 생성 및 저장
            verify(exactly = 1) { careerFieldingStatsRepository.save(any()) }
        }

        @Test
        fun `기존 시즌 수비 기록 시 CareerFieldingStats에 addSeason 호출되지 않음`() {
            // given: 시즌 통계 이미 존재
            val existingSeason = SeasonFieldingStats.create(testPlayer, 2024)
            val careerFielding = CareerFieldingStats.create(testPlayer)
            careerFielding.addSeason()

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns existingSeason
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(testPlayer.id) } returns careerFielding
            every { careerFieldingStatsRepository.save(any()) } answers { firstArg() }

            val initialSeasons = careerFielding.seasonsPlayed

            // when
            listener.onGameResultConfirmed(event)

            // then: addSeason 호출 안 됨 - seasonsPlayed 변화 없음
            assertThat(careerFielding.seasonsPlayed).isEqualTo(initialSeasons)
        }

        @Test
        fun `모든 기록이 없는 경우 아무것도 저장하지 않음`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameResultConfirmed(event)

            // then
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
            verify(exactly = 0) { careerBattingStatsRepository.save(any()) }
            verify(exactly = 0) { careerPitchingStatsRepository.save(any()) }
            verify(exactly = 0) { seasonFieldingStatsRepository.save(any()) }
            verify(exactly = 0) { careerFieldingStatsRepository.save(any()) }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val badEvent =
                GameResultConfirmedEvent(
                    gameId = 999L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 0,
                    awayScore = 0,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onGameResultConfirmed(badEvent)
            }
        }
    }

    @Nested
    @DisplayName("retryOnOptimisticLock - Optimistic Locking 재시도")
    inner class RetryOnOptimisticLock {
        @Test
        fun `OptimisticLockingFailureException 발생 시 재시도 후 성공`() {
            // given
            var callCount = 0

            // when
            StatsEventListener.retryOnOptimisticLock("test") {
                callCount++
                if (callCount < 3) {
                    throw ObjectOptimisticLockingFailureException("test", null)
                }
            }

            // then: 3번째에 성공
            assertThat(callCount).isEqualTo(3)
        }

        @Test
        fun `재시도 횟수 초과 시 예외 전파`() {
            // given & when & then
            assertThrows<ObjectOptimisticLockingFailureException> {
                StatsEventListener.retryOnOptimisticLock("test") {
                    throw ObjectOptimisticLockingFailureException("test", null)
                }
            }
        }

        @Test
        fun `첫 시도에 성공하면 한 번만 호출됨`() {
            // given
            var callCount = 0

            // when
            StatsEventListener.retryOnOptimisticLock("test") {
                callCount++
            }

            // then
            assertThat(callCount).isEqualTo(1)
        }

        @Test
        fun `OptimisticLockingFailureException이 아닌 예외는 즉시 전파`() {
            // given
            var callCount = 0

            // when & then
            assertThrows<IllegalStateException> {
                StatsEventListener.retryOnOptimisticLock("test") {
                    callCount++
                    throw IllegalStateException("다른 예외")
                }
            }

            // then: 재시도 없이 1회만 호출
            assertThat(callCount).isEqualTo(1)
        }

        @Test
        fun `타석 기록 이벤트에서 OptimisticLocking 충돌 시 재시도하여 성공`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            // 첫 번째 save 시 충돌, 두 번째에 성공
            every { seasonBattingStatsRepository.save(any()) } throws
                ObjectOptimisticLockingFailureException("conflict", null) andThen stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then: 2번 호출됨 (1번 실패 + 1번 성공)
            verify(exactly = 2) { seasonBattingStatsRepository.save(any()) }
        }
    }
}
