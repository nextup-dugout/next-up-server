package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.event.FieldingEventType
import com.nextup.core.domain.event.FieldingRecordUpdatedEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns null
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { playerRepository.findByIdOrNull(testPitcher.id) } returns testPitcher
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then: 타격/투수 통계 모두 자동 생성 후 저장 (생성 1회 + 갱신 1회 = save 2회씩)
            verify(exactly = 2) { seasonBattingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPlayer.id) }
            verify(exactly = 2) { seasonPitchingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPitcher.id) }
        }

        @Test
        fun `시즌 통계가 없는 신규 선수의 첫 타석 기록 시 통계가 올바르게 반영됨`() {
            // given
            val savedStats = mutableListOf<SeasonBattingStats>()
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { playerRepository.findByIdOrNull(testPitcher.id) } returns testPitcher
            every { seasonBattingStatsRepository.save(capture(savedStats)) } answers { firstArg() }
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns null
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(999L, 2024, 10L)
            } returns null
            every { playerRepository.findByIdOrNull(999L) } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 999L,
                    pitcherId = testPitcher.id,
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
                    pitcherId = testPitcher.id,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2023)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2023, 10L)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2023, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.DOUBLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            verify { seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2023, 10L) }
            assertThat(stats.doubles).isEqualTo(1)
        }

        @Test
        fun `타석 결과가 투수 시즌 통계에도 즉시 반영됨`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.WALK,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.walksAllowed).isEqualTo(1)
        }

        @Test
        fun `투수 시즌 통계가 없는 경우 자동 생성 후 투수 통계 갱신됨`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns null
            every { playerRepository.findByIdOrNull(testPitcher.id) } returns testPitcher
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then: 타격 통계 저장 1회, 투수 통계 자동 생성 + 갱신 = 2회
            verify(exactly = 1) { seasonBattingStatsRepository.save(any()) }
            verify(exactly = 2) { seasonPitchingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPitcher.id) }
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            pitchingStats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)

            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns null
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns null
            every { playerRepository.findByIdOrNull(testPlayer.id) } returns testPlayer
            every { playerRepository.findByIdOrNull(testPitcher.id) } returns testPitcher
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then: 타격/투수 통계 모두 자동 생성 후 저장 (생성 1회 + 갱신 1회 = save 2회씩)
            verify(exactly = 2) { seasonBattingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPlayer.id) }
            verify(exactly = 2) { seasonPitchingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPitcher.id) }
        }

        @Test
        fun `Undo 시 투수 통계도 역산됨`() {
            // given
            val battingStats = SeasonBattingStats.create(testPlayer, 2024)
            battingStats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            pitchingStats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns battingStats
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
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
    @DisplayName("onGameResultConfirmed - 경기 결과 확정 이벤트 처리")
    inner class OnGameResultConfirmed {
        private val mockBattingRecord = mockk<BattingRecord>(relaxed = true)
        private val mockPitchingRecord = mockk<PitchingRecord>(relaxed = true)
        private val mockFieldingRecord = mockk<FieldingRecord>(relaxed = true)
        private val mockGamePlayer = mockk<GamePlayer>()
        private val mockPitcherGamePlayer = mockk<GamePlayer>()
        private val mockFielderGamePlayer = mockk<GamePlayer>()
        private val mockBatterGameTeam = mockk<GameTeam>()
        private val mockPitcherGameTeam = mockk<GameTeam>()
        private val mockBatterTeam = mockk<com.nextup.core.domain.team.Team>()
        private val mockPitcherTeam = mockk<com.nextup.core.domain.team.Team>()

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

            every { mockPitchingRecord.gamePlayer } returns mockPitcherGamePlayer
            every { mockPitcherGamePlayer.player } returns testPitcher
            every { mockPitcherGamePlayer.gameTeam } returns mockPitcherGameTeam
            every { mockPitcherGameTeam.team } returns mockPitcherTeam
            every { mockPitcherTeam.id } returns 20L

            every { mockFieldingRecord.gamePlayer } returns mockFielderGamePlayer
            every { mockFielderGamePlayer.player } returns testPlayer
            // 기본적으로 수비 기록 없음 (수비 테스트에서만 오버라이드)
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            // L-7: 교차 검증용 기본 목 설정 (개별 테스트에서 오버라이드 가능)
            every { battingRecordRepository.findAllByPlayerIdAndYear(any(), any()) } returns emptyList()
            every { fieldingRecordRepository.findAllByPlayerIdAndYear(any(), any()) } returns emptyList()
        }

        @Test
        fun `경기 종료 시 SeasonPitchingStats가 신규 생성되고 기록이 누적됨`() {
            // given
            val careerPitching = CareerPitchingStats.create(testPitcher)

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
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
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
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
        fun `경기 종료 시 기존 SeasonPitchingStats에 실시간 갱신 필드가 중복 추가되지 않음`() {
            // given: 경기 중 실시간 갱신으로 피안타 3, 삼진 2, 볼넷 1이 이미 반영된 상태
            val seasonPitching = SeasonPitchingStats.create(testPitcher, 2024)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.DOUBLE)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.WALK)

            val careerPitching = CareerPitchingStats.create(testPitcher)
            careerPitching.addSeason()

            // PitchingRecord mock: 동일한 경기 결과를 반영
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)
            every { pitchingRecord.gamePlayer } returns mockPitcherGamePlayer
            every { pitchingRecord.hitsAllowed } returns 3
            every { pitchingRecord.strikeouts } returns 2
            every { pitchingRecord.walksAllowed } returns 1
            every { pitchingRecord.homeRunsAllowed } returns 1
            every { pitchingRecord.hitBatsmen } returns 0
            every { pitchingRecord.battersFaced } returns 6
            every { pitchingRecord.inningsPitchedOuts } returns 9 // 3이닝
            every { pitchingRecord.earnedRuns } returns 2
            every { pitchingRecord.runsAllowed } returns 3
            every { pitchingRecord.wildPitches } returns 1
            every { pitchingRecord.balks } returns 0
            every { pitchingRecord.isStartingPitcher } returns true
            every { pitchingRecord.pitchesThrown } returns null
            every { pitchingRecord.strikesThrown } returns null
            every { pitchingRecord.decision } returns com.nextup.core.domain.game.PitchingDecision.WIN

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(pitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns seasonPitching
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 실시간 갱신 필드는 중복 추가되지 않음 (applyLiveUpdate 값 그대로)
            assertThat(seasonPitching.hitsAllowed).isEqualTo(3)
            assertThat(seasonPitching.strikeouts).isEqualTo(2)
            assertThat(seasonPitching.walksAllowed).isEqualTo(1)
            assertThat(seasonPitching.homeRunsAllowed).isEqualTo(1)
            assertThat(seasonPitching.battersFaced).isEqualTo(6)

            // then: 경기 종료 시에만 확정되는 필드는 정상 추가됨
            assertThat(seasonPitching.gamesPlayed).isEqualTo(1)
            assertThat(seasonPitching.gamesStarted).isEqualTo(1)
            assertThat(seasonPitching.inningsPitchedOuts).isEqualTo(9)
            assertThat(seasonPitching.earnedRuns).isEqualTo(2)
            assertThat(seasonPitching.runsAllowed).isEqualTo(3)
            assertThat(seasonPitching.wildPitches).isEqualTo(1)
            assertThat(seasonPitching.wins).isEqualTo(1)
        }

        @Test
        fun `첫 시즌 투수 기록 시 CareerPitchingStats에 addSeason 호출됨`() {
            // given: 시즌 통계 없음(첫 시즌)
            val careerPitching = CareerPitchingStats.create(testPitcher)

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
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
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
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
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
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
    @DisplayName("@Retryable - Optimistic Locking 재시도 설정 검증")
    inner class RetryableConfiguration {
        @Test
        fun `OptimisticLockingFailureException 발생 시 직접 호출에서는 예외 전파됨`() {
            // NOTE: @Retryable은 Spring AOP 프록시를 통해서만 동작합니다.
            // 직접 호출 시에는 재시도 없이 예외가 그대로 전파됩니다.
            // 이 테스트는 재시도 없이 예외가 올바르게 전파되는지 확인합니다.
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(testPlayer.id, 2024, 10L)
            } returns stats
            every { seasonBattingStatsRepository.save(any()) } throws
                ObjectOptimisticLockingFailureException("conflict", null)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(testPitcher.id, 2024, 20L)
            } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = testPlayer.id,
                    pitcherId = testPitcher.id,
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

    @Nested
    @DisplayName("L-6: onFieldingRecordUpdated - 수비 기록 실시간 갱신")
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
    }

    @Nested
    @DisplayName("L-7: 경기 종료 시 통계 정합성 교차 검증")
    inner class ConsistencyVerification {
        @Test
        fun `경기 종료 시 타격 통계 정합성 검증이 수행됨 - 일치`() {
            // given
            val gamePlayer = mockk<GamePlayer>()
            every { gamePlayer.player } returns testPlayer

            val battingRecord = mockk<BattingRecord>(relaxed = true)
            every { battingRecord.gamePlayer } returns gamePlayer
            every { battingRecord.plateAppearances } returns 4
            every { battingRecord.hits } returns 2
            every { battingRecord.atBats } returns 3

            val seasonBattingStats = SeasonBattingStats.create(testPlayer, 2024)
            // 실시간 갱신으로 4타석, 3타수, 2안타를 시뮬레이션
            seasonBattingStats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            seasonBattingStats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            seasonBattingStats.applyLiveUpdate(PlateAppearanceResult.GROUND_OUT)
            seasonBattingStats.applyLiveUpdate(PlateAppearanceResult.WALK)

            every { battingRecordRepository.findAllByGameId(10L) } returns listOf(battingRecord)
            every { pitchingRecordRepository.findAllByGameId(10L) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(10L) } returns emptyList()
            every {
                seasonBattingStatsRepository.findByPlayerIdAndYear(testPlayer.id, 2024)
            } returns seasonBattingStats
            every {
                battingRecordRepository.findAllByPlayerIdAndYear(testPlayer.id, 2024)
            } returns listOf(battingRecord)
            every {
                fieldingRecordRepository.findAllByPlayerIdAndYear(any(), any())
            } returns emptyList()
            every { careerBattingStatsRepository.findByPlayerId(testPlayer.id) } returns null
            every { careerBattingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                GameResultConfirmedEvent(
                    gameId = 10L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when - 정합성 일치이면 경고 로그 없이 정상 완료
            listener.onGameResultConfirmed(event)

            // then: 정합성 검증 수행됨 (findAllByPlayerIdAndYear 호출)
            verify { battingRecordRepository.findAllByPlayerIdAndYear(testPlayer.id, 2024) }
        }
    }
}
