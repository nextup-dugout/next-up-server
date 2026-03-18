package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.event.RecordCorrectedEvent
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
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
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
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
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.time.LocalDateTime

@DisplayName("RecordCorrectionEventListener 테스트")
class RecordCorrectionEventListenerTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val seasonPitchingStatsRepository = mockk<SeasonPitchingStatsRepositoryPort>()
    private val seasonFieldingStatsRepository = mockk<SeasonFieldingStatsRepositoryPort>()
    private val careerBattingStatsRepository = mockk<CareerBattingStatsRepositoryPort>()
    private val careerPitchingStatsRepository = mockk<CareerPitchingStatsRepositoryPort>()
    private val careerFieldingStatsRepository = mockk<CareerFieldingStatsRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val gamePlayerRepository = mockk<GamePlayerRepositoryPort>()
    private val gameTeamRepository = mockk<GameTeamRepositoryPort>()
    private val gameEventRepository = mockk<GameEventRepositoryPort>()
    private val cacheManager = mockk<CacheManager>()
    private val mockCache = mockk<Cache>(relaxed = true)

    private val listener =
        RecordCorrectionEventListener(
            seasonBattingStatsRepository = seasonBattingStatsRepository,
            seasonPitchingStatsRepository = seasonPitchingStatsRepository,
            seasonFieldingStatsRepository = seasonFieldingStatsRepository,
            careerBattingStatsRepository = careerBattingStatsRepository,
            careerPitchingStatsRepository = careerPitchingStatsRepository,
            careerFieldingStatsRepository = careerFieldingStatsRepository,
            gameRepository = gameRepository,
            gamePlayerRepository = gamePlayerRepository,
            gameTeamRepository = gameTeamRepository,
            gameEventRepository = gameEventRepository,
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
    private val mockCompetition = mockk<Competition>()
    private val mockGamePlayer = mockk<GamePlayer>()
    private val mockTeam = mockk<Team>(relaxed = true)

    private fun createGameTeam(): GameTeam =
        GameTeam(
            game = mockGame,
            team = mockTeam,
            homeAway = HomeAway.HOME,
        )

    @BeforeEach
    fun setUp() {
        every { mockGame.scheduledAt } returns LocalDateTime.of(2024, 5, 15, 18, 0)
        every { mockGame.competition } returns mockCompetition
        every { mockCompetition.id } returns 100L
        every { gameRepository.findByIdOrNull(any()) } returns mockGame
        every { cacheManager.getCache(any()) } returns mockCache
        // 기본값: GamePlayer 없음 (GameTeam 갱신 건너뜀)
        every { gamePlayerRepository.findByGameIdAndPlayerId(any(), any()) } returns null
        // H6: gameEventId 없는 경우 기본 처리 (gameEventId = null → 건너뜀)
        every { gameEventRepository.findByIdOrNull(any()) } returns null
    }

    @Nested
    @DisplayName("타격 기록 정정 이벤트")
    inner class BattingCorrectionEvent {
        @Test
        fun `타격 기록 정정 시 시즌 및 커리어 통계에 델타가 반영됨`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            val careerStats = CareerBattingStats.create(testPlayer)
            seasonStats.applyFieldCorrection("plateAppearances", 15)
            seasonStats.applyFieldCorrection("atBats", 12)
            seasonStats.applyFieldCorrection("hits", 10)
            careerStats.applyFieldCorrection("plateAppearances", 15)
            careerStats.applyFieldCorrection("atBats", 12)
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
            seasonStats.applyFieldCorrection("plateAppearances", 10)
            seasonStats.applyFieldCorrection("atBats", 8)
            seasonStats.applyFieldCorrection("hits", 5)
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

            // then
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
            seasonStats.applyFieldCorrection("runsAllowed", 12)
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
            seasonStats.applyFieldCorrection("plateAppearances", 15)
            seasonStats.applyFieldCorrection("atBats", 12)
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
            verify(exactly = 1) { mockCache.evict(100L) }
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
    @DisplayName("수비 기록 정정 이벤트")
    inner class FieldingCorrectionEvent {
        @Test
        fun `수비 기록 정정 시 시즌 및 커리어 통계에 델타가 반영됨`() {
            // given
            val seasonStats = SeasonFieldingStats.create(testPlayer, 2024)
            val careerStats = CareerFieldingStats.create(testPlayer)
            seasonStats.applyFieldCorrection("putOuts", 10)
            careerStats.applyFieldCorrection("putOuts", 10)

            every { seasonFieldingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(1L) } returns careerStats
            every { careerFieldingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.FIELDING,
                    playerId = 1L,
                    fieldName = "putOuts",
                    oldValue = "2",
                    newValue = "5",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 5 - 2 = 3, so putOuts = 10 + 3 = 13
            assertThat(seasonStats.putOuts).isEqualTo(13)
            assertThat(careerStats.putOuts).isEqualTo(13)
            verify(exactly = 1) { seasonFieldingStatsRepository.save(seasonStats) }
            verify(exactly = 1) { careerFieldingStatsRepository.save(careerStats) }
        }

        @Test
        fun `수비 실책 감소 정정이 올바르게 반영됨`() {
            // given
            val seasonStats = SeasonFieldingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("errors", 5)

            every { seasonFieldingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.FIELDING,
                    playerId = 1L,
                    fieldName = "errors",
                    oldValue = "3",
                    newValue = "1",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 1 - 3 = -2, errors = 5 - 2 = 3
            assertThat(seasonStats.errors).isEqualTo(3)
        }

        @Test
        fun `수비 시즌 통계가 없으면 스탯 갱신을 건너뜀`() {
            // given
            every { seasonFieldingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerFieldingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.FIELDING,
                    playerId = 1L,
                    fieldName = "assists",
                    oldValue = "2",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 0) { seasonFieldingStatsRepository.save(any()) }
            verify(exactly = 0) { careerFieldingStatsRepository.save(any()) }
        }

        @Test
        fun `병살 관여 정정이 올바르게 반영됨`() {
            // given
            val seasonStats = SeasonFieldingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("doublePlays", 3)

            every { seasonFieldingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.FIELDING,
                    playerId = 1L,
                    fieldName = "doublePlays",
                    oldValue = "1",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 3 - 1 = 2, doublePlays = 3 + 2 = 5
            assertThat(seasonStats.doublePlays).isEqualTo(5)
        }

        @Test
        fun `포일 정정이 올바르게 반영됨`() {
            // given
            val seasonStats = SeasonFieldingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("passedBalls", 2)

            every { seasonFieldingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonFieldingStatsRepository.save(any()) } answers { firstArg() }
            every { careerFieldingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.FIELDING,
                    playerId = 1L,
                    fieldName = "passedBalls",
                    oldValue = "0",
                    newValue = "1",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 1 - 0 = 1, passedBalls = 2 + 1 = 3
            assertThat(seasonStats.passedBalls).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("GameEvent 타임라인 정정 (H6)")
    inner class GameEventCorrection {
        @Test
        fun `gameEventId가 있으면 GameEvent description이 갱신된다`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("plateAppearances", 10)
            seasonStats.applyFieldCorrection("atBats", 8)
            seasonStats.applyFieldCorrection("hits", 5)

            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val mockGameEvent = mockk<GameEvent>(relaxed = true)
            every { mockGameEvent.description } returns "헛스윙 삼진"
            every { gameEventRepository.findByIdOrNull(42L) } returns mockGameEvent
            every { gameEventRepository.save(any()) } answers { firstArg() }

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "2",
                    newValue = "3",
                    gameEventId = 42L,
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 1) { gameEventRepository.findByIdOrNull(42L) }
            verify(exactly = 1) { gameEventRepository.save(any()) }
        }

        @Test
        fun `gameEventId가 null이면 GameEvent 정정을 건너뜀`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("plateAppearances", 10)
            seasonStats.applyFieldCorrection("atBats", 8)
            seasonStats.applyFieldCorrection("hits", 5)

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
                    gameEventId = null,
                )

            // when
            listener.onRecordCorrected(event)

            // then: gameEventRepository.findByIdOrNull은 호출되지 않아야 함
            verify(exactly = 0) { gameEventRepository.findByIdOrNull(any()) }
            verify(exactly = 0) { gameEventRepository.save(any()) }
        }

        @Test
        fun `PITCHING 정정 시에는 GameEvent 정정을 건너뜀`() {
            // given
            val seasonStats = SeasonPitchingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("strikeouts", 10)

            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.PITCHING,
                    playerId = 1L,
                    fieldName = "strikeouts",
                    oldValue = "3",
                    newValue = "5",
                    gameEventId = 42L,
                )

            // when
            listener.onRecordCorrected(event)

            // then: PITCHING 정정은 GameEvent를 건드리지 않음
            verify(exactly = 0) { gameEventRepository.findByIdOrNull(any()) }
            verify(exactly = 0) { gameEventRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("델타 타입 안전성")
    inner class DeltaTypeSafety {
        @Test
        fun `소수 값이 전달되면 델타 0으로 처리되어 스탯 갱신을 건너뜀`() {
            // given
            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "2.5",
                    newValue = "3.7",
                )

            // when
            listener.onRecordCorrected(event)

            // then: 파싱 실패로 delta = 0, 스탯 갱신 건너뜀
            verify(exactly = 0) { seasonBattingStatsRepository.findByPlayerIdAndYear(any(), any()) }
        }

        @Test
        fun `빈 문자열이 전달되면 델타 0으로 처리됨`() {
            // given
            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.PITCHING,
                    playerId = 1L,
                    fieldName = "strikeouts",
                    oldValue = "",
                    newValue = "5",
                )

            // when
            listener.onRecordCorrected(event)

            // then: 파싱 실패로 delta = 0, 스탯 갱신 건너뜀
            verify(exactly = 0) { seasonPitchingStatsRepository.findByPlayerIdAndYear(any(), any()) }
        }

        @Test
        fun `비숫자 문자열이 전달되면 델타 0으로 처리됨`() {
            // given
            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.FIELDING,
                    playerId = 1L,
                    fieldName = "errors",
                    oldValue = "abc",
                    newValue = "2",
                )

            // when
            listener.onRecordCorrected(event)

            // then: 파싱 실패로 delta = 0, 스탯 갱신 건너뜀
            verify(exactly = 0) { seasonFieldingStatsRepository.findByPlayerIdAndYear(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Optimistic Locking 재시도")
    inner class OptimisticLockingRetry {
        @Test
        fun `기록 정정 시 OptimisticLocking 충돌이 발생하면 재시도하여 성공`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("plateAppearances", 15)
            seasonStats.applyFieldCorrection("atBats", 12)
            seasonStats.applyFieldCorrection("hits", 10)

            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            // 첫 번째 save 시 충돌, 두 번째에 성공
            every { seasonBattingStatsRepository.save(any()) } throws
                ObjectOptimisticLockingFailureException("conflict", null) andThen seasonStats
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

            // then: 2번 호출됨 (1번 실패 + 1번 성공)
            verify(exactly = 2) { seasonBattingStatsRepository.save(any()) }
        }

        @Test
        fun `재시도 횟수 초과 시 예외가 전파됨`() {
            // given
            val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
            seasonStats.applyFieldCorrection("walks", 10)

            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
            every { seasonBattingStatsRepository.save(any()) } throws
                ObjectOptimisticLockingFailureException("conflict", null)
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "walks",
                    oldValue = "2",
                    newValue = "3",
                )

            // when & then
            assertThrows<ObjectOptimisticLockingFailureException> {
                listener.onRecordCorrected(event)
            }
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

    @Nested
    @DisplayName("GameTeam 득점/안타 동기화")
    inner class GameTeamSyncTest {
        @Test
        fun `runs 필드 정정 시 GameTeam totalScore가 갱신됨`() {
            // given
            val gameTeam = createGameTeam()
            gameTeam.addScore(5)

            every { mockGamePlayer.gameTeam } returns gameTeam
            every { gamePlayerRepository.findByGameIdAndPlayerId(10L, 1L) } returns mockGamePlayer
            every { gameTeamRepository.save(any()) } answers { firstArg() }
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "runs",
                    oldValue = "1",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 3 - 1 = 2, totalScore = 5 + 2 = 7
            assertThat(gameTeam.totalScore).isEqualTo(7)
            verify(exactly = 1) { gameTeamRepository.save(gameTeam) }
        }

        @Test
        fun `runs 필드 음수 정정 시 GameTeam totalScore가 감소함`() {
            // given
            val gameTeam = createGameTeam()
            gameTeam.addScore(5)

            every { mockGamePlayer.gameTeam } returns gameTeam
            every { gamePlayerRepository.findByGameIdAndPlayerId(10L, 1L) } returns mockGamePlayer
            every { gameTeamRepository.save(any()) } answers { firstArg() }
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "runs",
                    oldValue = "3",
                    newValue = "1",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 1 - 3 = -2, totalScore = 5 - 2 = 3
            assertThat(gameTeam.totalScore).isEqualTo(3)
            verify(exactly = 1) { gameTeamRepository.save(gameTeam) }
        }

        @Test
        fun `runs 정정으로 totalScore가 0 미만이 되면 0으로 보호됨`() {
            // given
            val gameTeam = createGameTeam()
            gameTeam.addScore(1)

            every { mockGamePlayer.gameTeam } returns gameTeam
            every { gamePlayerRepository.findByGameIdAndPlayerId(10L, 1L) } returns mockGamePlayer
            every { gameTeamRepository.save(any()) } answers { firstArg() }
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "runs",
                    oldValue = "5",
                    newValue = "0",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 0 - 5 = -5, totalScore = maxOf(0, 1 - 5) = 0
            assertThat(gameTeam.totalScore).isEqualTo(0)
        }

        @Test
        fun `hits 필드 정정 시 GameTeam totalHits가 갱신됨`() {
            // given
            val gameTeam = createGameTeam()
            gameTeam.addHit()
            gameTeam.addHit()
            gameTeam.addHit()

            every { mockGamePlayer.gameTeam } returns gameTeam
            every { gamePlayerRepository.findByGameIdAndPlayerId(10L, 1L) } returns mockGamePlayer
            every { gameTeamRepository.save(any()) } answers { firstArg() }
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "hits",
                    oldValue = "1",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 3 - 1 = 2, totalHits = 3 + 2 = 5
            assertThat(gameTeam.totalHits).isEqualTo(5)
            verify(exactly = 1) { gameTeamRepository.save(gameTeam) }
        }

        @Test
        fun `homeRuns 필드 정정 시 GameTeam totalHits가 갱신됨`() {
            // given
            val gameTeam = createGameTeam()
            gameTeam.addHit()
            gameTeam.addHit()

            every { mockGamePlayer.gameTeam } returns gameTeam
            every { gamePlayerRepository.findByGameIdAndPlayerId(10L, 1L) } returns mockGamePlayer
            every { gameTeamRepository.save(any()) } answers { firstArg() }
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "homeRuns",
                    oldValue = "0",
                    newValue = "1",
                )

            // when
            listener.onRecordCorrected(event)

            // then: delta = 1 - 0 = 1, totalHits = 2 + 1 = 3
            assertThat(gameTeam.totalHits).isEqualTo(3)
            verify(exactly = 1) { gameTeamRepository.save(gameTeam) }
        }

        @Test
        fun `득점과 무관한 필드 정정 시 GameTeam은 갱신되지 않음`() {
            // given
            val gameTeam = createGameTeam()
            gameTeam.addScore(3)

            every { mockGamePlayer.gameTeam } returns gameTeam
            every { gamePlayerRepository.findByGameIdAndPlayerId(10L, 1L) } returns mockGamePlayer
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "walks",
                    oldValue = "1",
                    newValue = "2",
                )

            // when
            listener.onRecordCorrected(event)

            // then: walks는 GameTeam과 무관
            assertThat(gameTeam.totalScore).isEqualTo(3)
            verify(exactly = 0) { gameTeamRepository.save(any()) }
        }

        @Test
        fun `GamePlayer가 없으면 GameTeam 갱신을 건너뜀`() {
            // given
            every { gamePlayerRepository.findByGameIdAndPlayerId(10L, 1L) } returns null
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerBattingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.BATTING,
                    playerId = 1L,
                    fieldName = "runs",
                    oldValue = "1",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 0) { gameTeamRepository.save(any()) }
        }

        @Test
        fun `투구 기록 정정 시 GameTeam은 갱신되지 않음`() {
            // given
            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns null
            every { careerPitchingStatsRepository.findByPlayerId(1L) } returns null

            val event =
                RecordCorrectedEvent(
                    gameId = 10L,
                    correctionType = CorrectionType.PITCHING,
                    playerId = 1L,
                    fieldName = "earnedRuns",
                    oldValue = "1",
                    newValue = "3",
                )

            // when
            listener.onRecordCorrected(event)

            // then: PITCHING 타입은 GameTeam 갱신 없음
            verify(exactly = 0) { gameTeamRepository.save(any()) }
            verify(exactly = 0) { gamePlayerRepository.findByGameIdAndPlayerId(any(), any()) }
        }
    }
}
