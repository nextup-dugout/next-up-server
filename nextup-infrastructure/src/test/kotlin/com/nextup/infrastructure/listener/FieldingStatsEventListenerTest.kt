package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.event.FieldingEventType
import com.nextup.core.domain.event.FieldingRecordUpdatedEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
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

@DisplayName("FieldingStatsEventListener 테스트")
class FieldingStatsEventListenerTest {
    private val seasonFieldingStatsRepository = mockk<SeasonFieldingStatsRepositoryPort>()
    private val careerFieldingStatsRepository = mockk<CareerFieldingStatsRepositoryPort>()
    private val fieldingRecordRepository = mockk<FieldingRecordRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val playerRepository = mockk<PlayerRepositoryPort>()

    private val listener =
        FieldingStatsEventListener(
            seasonFieldingStatsRepository = seasonFieldingStatsRepository,
            careerFieldingStatsRepository = careerFieldingStatsRepository,
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

    private val mockGame = mockk<Game>()

    @BeforeEach
    fun setUp() {
        every { mockGame.scheduledAt } returns LocalDateTime.of(2024, 5, 15, 18, 0)
        every { gameRepository.findByIdOrNull(any()) } returns mockGame
    }

    @Nested
    @DisplayName("onFieldingRecordUpdated - 수비 기록 실시간 갱신")
    inner class OnFieldingRecordUpdated {
        @Test
        fun `PUT_OUT 이벤트로 시즌 수비 통계가 갱신됨`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2024)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonFieldingStatsRepository.save(any()) } returns stats

            val event =
                FieldingRecordUpdatedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    type = FieldingEventType.PUT_OUT,
                )

            // when
            listener.onFieldingRecordUpdated(event)

            // then
            assertThat(stats.putOuts).isEqualTo(1)
            verify { seasonFieldingStatsRepository.save(stats) }
        }

        @Test
        fun `ERROR 이벤트로 실책 통계가 갱신됨`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2024)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonFieldingStatsRepository.save(any()) } returns stats

            val event =
                FieldingRecordUpdatedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    type = FieldingEventType.ERROR,
                )

            // when
            listener.onFieldingRecordUpdated(event)

            // then
            assertThat(stats.errors).isEqualTo(1)
        }

        @Test
        fun `isRevert=true이면 역산 처리됨`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2024)
            stats.applyLiveFieldingUpdate(FieldingEventType.ASSIST)

            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonFieldingStatsRepository.save(any()) } returns stats

            val event =
                FieldingRecordUpdatedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    type = FieldingEventType.ASSIST,
                    isRevert = true,
                )

            // when
            listener.onFieldingRecordUpdated(event)

            // then
            assertThat(stats.assists).isEqualTo(0)
        }

        @Test
        fun `시즌 수비 통계가 없는 경우 자동 생성 후 갱신`() {
            // given
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                FieldingRecordUpdatedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    type = FieldingEventType.PUT_OUT,
                )

            // when
            listener.onFieldingRecordUpdated(event)

            // then: 자동 생성 1회 + 갱신 1회 = save 2회
            verify(exactly = 2) { seasonFieldingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPlayer.id) }
        }

        @Test
        fun `선수를 찾을 수 없는 경우 PlayerNotFoundException 발생`() {
            // given
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(999L, 2024)
            } returns null
            every { playerRepository.findByIdOrNull(999L) } returns null

            val event =
                FieldingRecordUpdatedEvent(
                    gameId = 10L,
                    playerId = 999L,
                    type = FieldingEventType.PUT_OUT,
                )

            // when & then
            assertThrows<PlayerNotFoundException> {
                listener.onFieldingRecordUpdated(event)
            }
        }

        @Test
        fun `DOUBLE_PLAY 이벤트로 병살 관여 통계가 갱신됨`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2024)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonFieldingStatsRepository.save(any()) } returns stats

            val event =
                FieldingRecordUpdatedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    type = FieldingEventType.DOUBLE_PLAY,
                )

            // when
            listener.onFieldingRecordUpdated(event)

            // then
            assertThat(stats.doublePlays).isEqualTo(1)
        }

        @Test
        fun `PASSED_BALL 이벤트로 포일 통계가 갱신됨`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2024)
            every {
                seasonFieldingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns stats
            every { seasonFieldingStatsRepository.save(any()) } returns stats

            val event =
                FieldingRecordUpdatedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    type = FieldingEventType.PASSED_BALL,
                )

            // when
            listener.onFieldingRecordUpdated(event)

            // then
            assertThat(stats.passedBalls).isEqualTo(1)
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                FieldingRecordUpdatedEvent(
                    gameId = 999L,
                    playerId = testPlayer.id,
                    type = FieldingEventType.PUT_OUT,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onFieldingRecordUpdated(event)
            }
        }
    }

    @Nested
    @DisplayName("onGameResultConfirmed - 경기 결과 확정 이벤트 처리 (수비)")
    inner class OnGameResultConfirmed {
        private val mockFieldingRecord = mockk<FieldingRecord>(relaxed = true)
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
            every { mockFieldingRecord.gamePlayer } returns mockFielderGamePlayer
            every { mockFielderGamePlayer.player } returns testPlayer
            // L-7: 교차 검증용 기본 목 설정
            every { fieldingRecordRepository.findAllByPlayerIdAndYear(any(), any()) } returns emptyList()
        }

        @Test
        fun `경기 종료 시 SeasonFieldingStats가 신규 생성되고 기록이 누적됨`() {
            // given
            val careerFielding = CareerFieldingStats.create(testPlayer)

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
            // given
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
        fun `수비 기록이 없는 경우 아무것도 저장하지 않음`() {
            // given
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameResultConfirmed(event)

            // then
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
}
