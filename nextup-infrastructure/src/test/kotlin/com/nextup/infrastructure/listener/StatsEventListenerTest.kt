package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
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
import java.time.LocalDateTime

@DisplayName("StatsEventListener 테스트")
class StatsEventListenerTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val seasonPitchingStatsRepository = mockk<SeasonPitchingStatsRepositoryPort>()
    private val careerBattingStatsRepository = mockk<CareerBattingStatsRepositoryPort>()
    private val careerPitchingStatsRepository = mockk<CareerPitchingStatsRepositoryPort>()
    private val battingRecordRepository = mockk<BattingRecordRepositoryPort>()
    private val pitchingRecordRepository = mockk<PitchingRecordRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()

    private val listener =
        StatsEventListener(
            seasonBattingStatsRepository = seasonBattingStatsRepository,
            seasonPitchingStatsRepository = seasonPitchingStatsRepository,
            careerBattingStatsRepository = careerBattingStatsRepository,
            careerPitchingStatsRepository = careerPitchingStatsRepository,
            battingRecordRepository = battingRecordRepository,
            pitchingRecordRepository = pitchingRecordRepository,
            gameRepository = gameRepository,
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

    @Nested
    @DisplayName("onGameResultConfirmed - 경기 결과 확정 이벤트 처리")
    inner class OnGameResultConfirmed {
        private val mockBattingRecord = mockk<BattingRecord>(relaxed = true)
        private val mockPitchingRecord = mockk<PitchingRecord>(relaxed = true)
        private val mockGamePlayer = mockk<GamePlayer>()
        private val mockPitcherGamePlayer = mockk<GamePlayer>()

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
        fun `타격 기록과 투수 기록이 모두 없는 경우 아무것도 저장하지 않음`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameResultConfirmed(event)

            // then
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
            verify(exactly = 0) { careerBattingStatsRepository.save(any()) }
            verify(exactly = 0) { careerPitchingStatsRepository.save(any()) }
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
}
