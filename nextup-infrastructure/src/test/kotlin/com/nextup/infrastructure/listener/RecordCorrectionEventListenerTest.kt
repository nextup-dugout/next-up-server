package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.event.RecordCorrectedEvent
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
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
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import java.time.LocalDateTime

@DisplayName("RecordCorrectionEventListener 테스트")
class RecordCorrectionEventListenerTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val seasonPitchingStatsRepository = mockk<SeasonPitchingStatsRepositoryPort>()
    private val careerBattingStatsRepository = mockk<CareerBattingStatsRepositoryPort>()
    private val careerPitchingStatsRepository = mockk<CareerPitchingStatsRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val cacheManager = mockk<CacheManager>()
    private val standingsCache = mockk<Cache>()

    private val listener =
        RecordCorrectionEventListener(
            seasonBattingStatsRepository = seasonBattingStatsRepository,
            seasonPitchingStatsRepository = seasonPitchingStatsRepository,
            careerBattingStatsRepository = careerBattingStatsRepository,
            careerPitchingStatsRepository = careerPitchingStatsRepository,
            gameRepository = gameRepository,
            cacheManager = cacheManager,
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

    private val mockCompetition = mockk<com.nextup.core.domain.competition.Competition>()

    @BeforeEach
    fun setUp() {
        every { mockGame.scheduledAt } returns LocalDateTime.of(2024, 5, 15, 18, 0)
        every { mockGame.competition } returns mockCompetition
        every { mockCompetition.id } returns 100L
        every { gameRepository.findByIdOrNull(any()) } returns mockGame
        every { cacheManager.getCache(any()) } returns standingsCache
        every { standingsCache.evict(any()) } returns Unit
    }

    @Nested
    @DisplayName("타격 기록 정정 이벤트")
    inner class BattingCorrectionEvent {
        @Test
        fun `타격 기록 정정 시 시즌 및 커리어 통계에 델타가 반영됨`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            val careerStats = CareerBattingStats.create(testPlayer)
            // 기존 값 설정: hits = 10 (addGameRecord 대신 applyFieldCorrection으로 세팅)
            seasonStats.applyFieldCorrection("hits", 10)
            careerStats.applyFieldCorrection("hits", 10)

            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns careerStats
            every { careerBattingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "2",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 3 - 2 = 1, so hits = 10 + 1 = 11
            assertThat(seasonStats.hits).isEqualTo(11)
            assertThat(careerStats.hits).isEqualTo(11)
            verify(exactly = 1) { seasonBattingStatsRepository.save(seasonStats) }
            verify(exactly = 1) { careerBattingStatsRepository.save(careerStats) }
        }

        @Test
        fun `음수 델타 적용 시 통계가 감소됨`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("homeRuns", 5)

            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "homeRuns",
                    oldValue = "3",
                    newValue = "1",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 1 - 3 = -2, so homeRuns = 5 - 2 = 3
            assertThat(seasonStats.homeRuns).isEqualTo(3)
        }

        @Test
        fun `델타가 0이면 스탯 갱신을 건너뜀`() {
            // given
            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "3",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then: 아무 repository 호출도 없어야 함
            verify(exactly = 0) { seasonBattingStatsRepository.findByPlayerIdAndYear(any(), any()) }
            verify(exactly = 0) { careerBattingStatsRepository.findByPlayerId(any()) }
        }

        @Test
        fun `시즌 통계가 없으면 시즌 스탯 갱신을 건너뜀`() {
            // given
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "2",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 0) { seasonBattingStatsRepository.save(any()) }
            verify(exactly = 0) { careerBattingStatsRepository.save(any()) }
        }

        @Test
        fun `음수 방지로 0 미만이 되지 않음`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("walks", 1)

            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "walks",
                    oldValue = "5",
                    newValue = "1",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 1 - 5 = -4, walks = maxOf(0, 1 - 4) = 0
            assertThat(seasonStats.walks).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("투수 기록 정정 이벤트")
    inner class PitchingCorrectionEvent {
        @Test
        fun `투수 기록 정정 시 시즌 및 커리어 통계에 델타가 반영됨`() {
            // given
            val seasonStats = SeasonPitchingStats.create(testPlayer, 2024)
            val careerStats = CareerPitchingStats.create(testPlayer)
            seasonStats.applyFieldCorrection("strikeouts", 20)
            careerStats.applyFieldCorrection("strikeouts", 20)

            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(1L) } returns careerStats
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.PITCHING,
                    playerId = 1L,
                    fieldName = "strikeouts",
                    oldValue = "5",
                    newValue = "7",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 7 - 5 = 2, so strikeouts = 20 + 2 = 22
            assertThat(seasonStats.strikeouts).isEqualTo(22)
            assertThat(careerStats.strikeouts).isEqualTo(22)
            verify(exactly = 1) { seasonPitchingStatsRepository.save(seasonStats) }
            verify(exactly = 1) { careerPitchingStatsRepository.save(careerStats) }
        }

        @Test
        fun `투수 시즌 통계가 없으면 스탯 갱신을 건너뜀`() {
            // given
            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerPitchingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.PITCHING,
                    playerId = 1L,
                    fieldName = "earnedRuns",
                    oldValue = "2",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
            verify(exactly = 0) { careerPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `자책점 감소 정정이 올바르게 반영됨`() {
            // given
            val seasonStats = SeasonPitchingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("earnedRuns", 10)

            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.PITCHING,
                    playerId = 1L,
                    fieldName = "earnedRuns",
                    oldValue = "3",
                    newValue = "1",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 1 - 3 = -2, earnedRuns = 10 - 2 = 8
            assertThat(seasonStats.earnedRuns).isEqualTo(8)
        }
    }

    @Nested
    @DisplayName("순위 캐시 무효화")
    inner class StandingsCacheEviction {
        @Test
        fun `기록 정정 시 해당 대회의 순위 캐시가 무효화됨`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("hits", 10)

            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "2",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 1) { cacheManager.getCache("standings") }
            verify(exactly = 1) { standingsCache.evict(100L) }
        }

        @Test
        fun `델타가 0이면 캐시 무효화도 건너뜀`() {
            // given
            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "3",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 0) { cacheManager.getCache(any()) }
        }
    }

    @Nested
    @DisplayName("공통 예외 처리")
    inner class CommonExceptionHandling {
        @Test
        fun `경기를 찾을 수 없으면 GameNotFoundException이 발생함`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 999L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "2",
                    newValue = "3",
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onRecordCorrected(event)
            }
        }
    }
}
