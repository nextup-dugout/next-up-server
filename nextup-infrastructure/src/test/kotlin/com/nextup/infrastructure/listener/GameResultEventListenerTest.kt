package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.CompetitionCompletedEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.infrastructure.config.CacheConfig
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
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameResultEventListener 테스트")
class GameResultEventListenerTest {
    private val gameRepository = mockk<GameRepositoryPort>()
    private val competitionRepository = mockk<CompetitionRepositoryPort>()
    private val cacheManager = mockk<CacheManager>()
    private val standingsCache = mockk<Cache>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val listener =
        GameResultEventListener(gameRepository, competitionRepository, cacheManager, eventPublisher)

    private lateinit var competition: Competition
    private lateinit var game: Game

    @BeforeEach
    fun setUp() {
        competition = createCompetition(1L, CompetitionStatus.IN_PROGRESS)
        game = createGame(10L, competition, GameStatus.FINISHED)

        every { cacheManager.getCache(CacheConfig.STANDINGS_CACHE) } returns standingsCache
        every { standingsCache.evict(any<Long>()) } returns Unit
    }

    @Nested
    @DisplayName("onGameResultConfirmed - 경기 결과 확정 이벤트 처리")
    inner class OnGameResultConfirmed {
        @Test
        fun `경기가 존재하지 않으면 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            val event =
                GameResultConfirmedEvent(
                    gameId = 999L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onGameResultConfirmed(event)
            }
        }

        @Test
        fun `모든 경기가 완료되면 대회를 자동 완료 처리`() {
            // given
            every { gameRepository.findByIdOrNull(10L) } returns game
            every { gameRepository.countByCompetitionId(1L) } returns 5L
            every { gameRepository.countCompletedOrCancelledByCompetitionId(1L) } returns 5L
            every { competitionRepository.save(any()) } returnsArgument 0

            val event =
                GameResultConfirmedEvent(
                    gameId = 10L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when
            listener.onGameResultConfirmed(event)

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.COMPLETED)
            verify { competitionRepository.save(competition) }
            verify {
                eventPublisher.publishEvent(
                    match<CompetitionCompletedEvent> {
                        it.competitionId == 1L && it.competitionName == "테스트 대회 1"
                    },
                )
            }
        }

        @Test
        fun `아직 완료되지 않은 경기가 있으면 대회를 완료하지 않음`() {
            // given
            every { gameRepository.findByIdOrNull(10L) } returns game
            every { gameRepository.countByCompetitionId(1L) } returns 5L
            every { gameRepository.countCompletedOrCancelledByCompetitionId(1L) } returns 4L

            val event =
                GameResultConfirmedEvent(
                    gameId = 10L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when
            listener.onGameResultConfirmed(event)

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.IN_PROGRESS)
            verify(exactly = 0) { competitionRepository.save(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any<CompetitionCompletedEvent>()) }
        }

        @Test
        fun `대회가 IN_PROGRESS 상태가 아니면 자동 완료 처리 건너뜀`() {
            // given
            val scheduledCompetition = createCompetition(2L, CompetitionStatus.SCHEDULED)
            val scheduledGame = createGame(20L, scheduledCompetition, GameStatus.FINISHED)

            every { gameRepository.findByIdOrNull(20L) } returns scheduledGame

            val event =
                GameResultConfirmedEvent(
                    gameId = 20L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 3,
                    awayScore = 1,
                )

            // when
            listener.onGameResultConfirmed(event)

            // then
            verify(exactly = 0) { gameRepository.countByCompetitionId(any()) }
            verify(exactly = 0) { competitionRepository.save(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any<CompetitionCompletedEvent>()) }
        }

        @Test
        fun `대회에 경기가 없으면 자동 완료 처리 건너뜀`() {
            // given
            every { gameRepository.findByIdOrNull(10L) } returns game
            every { gameRepository.countByCompetitionId(1L) } returns 0L
            every { gameRepository.countCompletedOrCancelledByCompetitionId(1L) } returns 0L

            val event =
                GameResultConfirmedEvent(
                    gameId = 10L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when
            listener.onGameResultConfirmed(event)

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.IN_PROGRESS)
            verify(exactly = 0) { competitionRepository.save(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any<CompetitionCompletedEvent>()) }
        }
    }

    @Nested
    @DisplayName("evictStandingsCache - 순위 캐시 무효화")
    inner class EvictStandingsCache {
        @Test
        fun `경기 결과 확정 후 해당 대회의 순위 캐시를 무효화`() {
            // given
            every { gameRepository.findByIdOrNull(10L) } returns game

            val event =
                GameResultConfirmedEvent(
                    gameId = 10L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when
            listener.evictStandingsCache(event)

            // then
            verify { cacheManager.getCache(CacheConfig.STANDINGS_CACHE) }
            verify { standingsCache.evict(1L) }
        }

        @Test
        fun `경기를 찾을 수 없으면 캐시 무효화 없이 반환`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            val event =
                GameResultConfirmedEvent(
                    gameId = 999L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when
            listener.evictStandingsCache(event)

            // then
            verify(exactly = 0) { standingsCache.evict(any<Long>()) }
        }
    }

    // Helper methods
    private fun createCompetition(
        id: Long,
        status: CompetitionStatus,
    ): Competition {
        val competition =
            Competition(
                league = mockk(relaxed = true),
                name = "테스트 대회 $id",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = status,
            )
        val idField = Competition::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(competition, id)
        return competition
    }

    private fun createGame(
        id: Long,
        competition: Competition,
        status: GameStatus,
    ): Game {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        val homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
            status = status,
            totalInnings = 9,
            id = id,
        )
    }
}
