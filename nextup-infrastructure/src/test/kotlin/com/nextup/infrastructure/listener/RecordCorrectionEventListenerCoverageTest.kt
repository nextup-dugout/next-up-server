package com.nextup.infrastructure.listener

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.event.RecordCorrectedEvent
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.SeasonBattingStats
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import java.time.LocalDateTime

@DisplayName("RecordCorrectionEventListener 캐시 커버리지 테스트")
class RecordCorrectionEventListenerCoverageTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val seasonPitchingStatsRepository = mockk<SeasonPitchingStatsRepositoryPort>()
    private val seasonFieldingStatsRepository = mockk<SeasonFieldingStatsRepositoryPort>()
    private val careerBattingStatsRepository = mockk<CareerBattingStatsRepositoryPort>()
    private val careerPitchingStatsRepository = mockk<CareerPitchingStatsRepositoryPort>()
    private val careerFieldingStatsRepository = mockk<CareerFieldingStatsRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val gamePlayerRepository = mockk<GamePlayerRepositoryPort>()
    private val gameTeamRepository = mockk<GameTeamRepositoryPort>()
    private val gameEventRepository = mockk<GameEventRepositoryPort>(relaxed = true)
    private val cacheManager = mockk<CacheManager>()

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

    @Test
    fun `evictStandingsCache에서 게임을 찾을 수 없으면 캐시 무효화를 건너뜄다`() {
        // given
        val mockGame = mockk<Game>()
        val mockCompetition = mockk<Competition>()
        every { mockGame.scheduledAt } returns LocalDateTime.of(2024, 5, 15, 18, 0)
        every { mockGame.competition } returns mockCompetition
        every { mockCompetition.id } returns 100L

        // resolveYear에서는 찾지만, evictStandingsCache에서는 못 찾음
        every { gameRepository.findByIdOrNull(10L) } returnsMany listOf(mockGame, null)

        val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
        seasonStats.applyFieldCorrection("plateAppearances", 10)
        seasonStats.applyFieldCorrection("atBats", 5)
        every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
        every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
        every { careerBattingStatsRepository.findByPlayerId(1L) } returns null
        every { gamePlayerRepository.findByGameIdAndPlayerId(any(), any()) } returns null

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
        verify(exactly = 0) { cacheManager.getCache(any()) }
    }

    @Test
    fun `cacheManager에서 캐시를 찾을 수 없으면 evict를 호출하지 않는다`() {
        // given
        val mockGame = mockk<Game>()
        val mockCompetition = mockk<Competition>()
        every { mockGame.scheduledAt } returns LocalDateTime.of(2024, 5, 15, 18, 0)
        every { mockGame.competition } returns mockCompetition
        every { mockCompetition.id } returns 100L
        every { gameRepository.findByIdOrNull(any()) } returns mockGame
        every { cacheManager.getCache(any()) } returns null

        val seasonStats = SeasonBattingStats.create(testPlayer, 2024)
        seasonStats.applyFieldCorrection("plateAppearances", 10)
        seasonStats.applyFieldCorrection("atBats", 5)
        every { seasonBattingStatsRepository.findByPlayerIdAndYear(1L, 2024) } returns seasonStats
        every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }
        every { careerBattingStatsRepository.findByPlayerId(1L) } returns null
        every { gamePlayerRepository.findByGameIdAndPlayerId(any(), any()) } returns null

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
        verify(exactly = 1) { cacheManager.getCache(any()) }
    }
}
