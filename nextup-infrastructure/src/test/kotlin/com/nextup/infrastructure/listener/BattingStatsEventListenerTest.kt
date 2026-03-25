package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
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
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.time.LocalDateTime

@DisplayName("BattingStatsEventListener 테스트")
class BattingStatsEventListenerTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val careerBattingStatsRepository = mockk<CareerBattingStatsRepositoryPort>()
    private val battingRecordRepository = mockk<BattingRecordRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val playerRepository = mockk<PlayerRepositoryPort>()

    private val listener =
        BattingStatsEventListener(
            seasonBattingStatsRepository = seasonBattingStatsRepository,
            careerBattingStatsRepository = careerBattingStatsRepository,
            battingRecordRepository = battingRecordRepository,
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
    private val mockCompetition = mockk<Competition>()

    @BeforeEach
    fun setUp() {
        every { mockGame.scheduledAt } returns LocalDateTime.of(2024, 5, 15, 18, 0)
        every { mockGame.competition } returns mockCompetition
        every { mockCompetition.type } returns CompetitionType.LEAGUE
        every { gameRepository.findByIdOrNull(any()) } returns mockGame
    }

    @Nested
    @DisplayName("onPlateAppearanceRecorded - 타석 결과 기록 이벤트 처리 (타격)")
    inner class OnPlateAppearanceRecorded {
        @Test
        fun `타석 결과가 시즌 타격 통계에 즉시 반영됨`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then: 자동 생성 1회 + 갱신 1회 = save 2회
            verify(exactly = 2) { seasonBattingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPlayer.id) }
        }

        @Test
        fun `시즌 통계가 없는 신규 선수의 첫 타석 기록 시 통계가 올바르게 반영됨`() {
            // given
            val savedStats = mutableListOf<SeasonBattingStats>()
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { seasonBattingStatsRepository.save(capture(savedStats)) } answers { firstArg() }

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(999L, 2024, 10L, any())
            } returns null
            every { playerRepository.findByIdOrNull(999L) } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 999L,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2023,
                    10L,
                    any()
                )
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.DOUBLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            verify {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2023,
                    10L,
                    any()
                )
            }
            assertThat(stats.doubles).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("onPlateAppearanceUndone - 타석 결과 취소 이벤트 처리 (타격)")
    inner class OnPlateAppearanceUndone {
        @Test
        fun `타석 결과가 시즌 타격 통계에서 역산됨`() {
            // given: 이미 단타가 반영된 통계
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then: 자동 생성 1회 + 갱신 1회 = save 2회
            verify(exactly = 2) { seasonBattingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPlayer.id) }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 999L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onPlateAppearanceUndone(event)
            }
        }
    }

    @Nested
    @DisplayName("onGameResultConfirmed - 경기 결과 확정 이벤트 처리 (타격)")
    inner class OnGameResultConfirmed {
        private val mockBattingRecord = mockk<BattingRecord>(relaxed = true)
        private val mockGamePlayer = mockk<GamePlayer>()
        private val mockBatterGameTeam = mockk<GameTeam>()
        private val mockBatterTeam = mockk<com.nextup.core.domain.team.Team>()

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
            every { mockGamePlayer.gameTeam } returns mockBatterGameTeam
            every { mockBatterGameTeam.team } returns mockBatterTeam
            every { mockBatterTeam.id } returns 10L
        }

        @Test
        fun `경기 종료 시 CareerBattingStats가 신규 생성되고 기록이 누적됨`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
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
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
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
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
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
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
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
        fun `타격 기록이 없는 경우 아무것도 저장하지 않음`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameResultConfirmed(event)

            // then
            verify(exactly = 0) { careerBattingStatsRepository.save(any()) }
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
    @DisplayName("@Retryable - Optimistic Locking 재시도 설정 검증")
    inner class RetryableConfiguration {
        @Test
        fun `OptimisticLockingFailureException 발생 시 직접 호출에서는 예외 전파됨`() {
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPlayer.id,
                    2024,
                    10L,
                    any()
                )
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } throws
                ObjectOptimisticLockingFailureException("conflict", null)

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = 2L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then: 직접 호출이므로 @Retryable 없이 예외 전파
            assertThrows<ObjectOptimisticLockingFailureException> {
                listener.onPlateAppearanceRecorded(event)
            }
        }
    }
}
