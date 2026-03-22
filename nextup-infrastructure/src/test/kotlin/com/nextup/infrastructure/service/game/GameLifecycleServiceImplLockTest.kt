package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.PitchingDecisionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameLifecycleServiceImpl - Lock/Unlock 테스트")
class GameLifecycleServiceImplLockTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var pitchingDecisionService: PitchingDecisionService
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var service: GameLifecycleServiceImpl

    private lateinit var competition: Competition
    private lateinit var league: League

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gameTeamRepository = mockk()
        pitchingRecordRepository = mockk()
        pitchingDecisionService = mockk(relaxed = true)
        eventPublisher = mockk(relaxed = true)
        service =
            GameLifecycleServiceImpl(
                gameRepository,
                gameTeamRepository,
                pitchingRecordRepository,
                pitchingDecisionService,
                eventPublisher,
            )

        val association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
            )
    }

    @Nested
    @DisplayName("lockGame")
    inner class LockGameTest {
        @Test
        @DisplayName("경기를 비관적 락으로 잠금하고 저장된 Game을 반환한다")
        fun locksGameAndReturnsSaved() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game = createGame(gameId, GameStatus.SCHEDULED)

            every { gameRepository.findByIdForUpdate(gameId) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.lockGame(gameId, scorerId)

            // then
            assertThat(result.scorerId).isEqualTo(scorerId)
            verify(exactly = 1) { gameRepository.findByIdForUpdate(gameId) }
            verify(exactly = 1) { gameRepository.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 경기를 잠금하면 예외가 발생한다")
        fun throwsWhenGameNotFound() {
            // given
            val gameId = 999L
            every { gameRepository.findByIdForUpdate(gameId) } returns null

            // when & then
            assertThatThrownBy { service.lockGame(gameId, 100L) }
                .isInstanceOf(GameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("unlockGame")
    inner class UnlockGameTest {
        @Test
        @DisplayName("경기 잠금을 해제하고 저장된 Game을 반환한다")
        fun unlocksGameAndReturnsSaved() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game = createGame(gameId, GameStatus.SCHEDULED, scorerId = scorerId)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.unlockGame(gameId, scorerId)

            // then
            assertThat(result.scorerId).isNull()
            verify(exactly = 1) { gameRepository.findByIdOrNull(gameId) }
            verify(exactly = 1) { gameRepository.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 경기의 잠금을 해제하면 예외가 발생한다")
        fun throwsWhenGameNotFound() {
            // given
            val gameId = 999L
            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when & then
            assertThatThrownBy { service.unlockGame(gameId, 100L) }
                .isInstanceOf(GameNotFoundException::class.java)
        }
    }

    private fun createGame(
        id: Long,
        status: GameStatus,
        scorerId: Long? = null,
    ): Game {
        val homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            status = status,
            scorerId = scorerId,
            id = id,
        )
    }
}
