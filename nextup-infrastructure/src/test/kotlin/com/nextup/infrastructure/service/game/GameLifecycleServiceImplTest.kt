package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.event.GamePostponedEvent
import com.nextup.core.domain.event.GameRescheduledEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.TiebreakerResult
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.PitchingDecisionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime

@DisplayName("GameLifecycleServiceImpl 테스트")
class GameLifecycleServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var pitchingDecisionService: PitchingDecisionService
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var service: GameLifecycleServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gameTeamRepository = mockk()
        pitchingRecordRepository = mockk()
        pitchingDecisionService = mockk(relaxed = true)
        eventPublisher = mockk(relaxed = true)
        service =
            GameLifecycleServiceImpl(
                gameRepository = gameRepository,
                gameTeamRepository = gameTeamRepository,
                pitchingRecordRepository = pitchingRecordRepository,
                pitchingDecisionService = pitchingDecisionService,
                eventPublisher = eventPublisher,
            )
    }

    @Nested
    @DisplayName("postponeGame")
    inner class PostponeGameTest {
        @Test
        @DisplayName("예정 상태의 경기를 연기하고 이벤트를 발행한다")
        fun postponeScheduledGameSuccessfully() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(homeTeam, awayTeam)

            val eventSlot = slot<GamePostponedEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            // when
            val result = service.postponeGame(gameId, newScheduledAt, "우천")

            // then
            assertThat(result).isEqualTo(game)
            verify(exactly = 1) { game.postpone(newScheduledAt, "우천") }
            verify(exactly = 1) { gameRepository.save(game) }
            verify(exactly = 1) { eventPublisher.publishEvent(any<GamePostponedEvent>()) }

            assertThat(eventSlot.captured.gameId).isEqualTo(gameId)
            assertThat(eventSlot.captured.homeTeamId).isEqualTo(10L)
            assertThat(eventSlot.captured.awayTeamId).isEqualTo(20L)
            assertThat(eventSlot.captured.newScheduledAt).isEqualTo(newScheduledAt)
        }

        @Test
        @DisplayName("reason 없이 연기할 수 있다")
        fun postponeGameWithoutReason() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(homeTeam, awayTeam)

            // when
            val result = service.postponeGame(gameId, newScheduledAt, null)

            // then
            assertThat(result).isEqualTo(game)
            verify(exactly = 1) { game.postpone(newScheduledAt, null) }
        }

        @Test
        @DisplayName("진행 중인 경기는 연기할 수 없다")
        fun cannotPostponeInProgressGame() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)
            val game = createGame(gameId, status = GameStatus.IN_PROGRESS)

            every { gameRepository.findByIdOrNull(gameId) } returns game

            // when & then
            assertThatThrownBy { service.postponeGame(gameId, newScheduledAt, null) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정 상태의 경기만 연기할 수 있습니다")
        }

        @Test
        @DisplayName("종료된 경기는 연기할 수 없다")
        fun cannotPostponeFinishedGame() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)
            val game = createGame(gameId, status = GameStatus.FINISHED)

            every { gameRepository.findByIdOrNull(gameId) } returns game

            // when & then
            assertThatThrownBy { service.postponeGame(gameId, newScheduledAt, null) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정 상태의 경기만 연기할 수 있습니다")
        }

        @Test
        @DisplayName("존재하지 않는 경기는 GameNotFoundException을 던진다")
        fun postponeNonExistentGameThrowsException() {
            // given
            val gameId = 999L
            val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)

            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when & then
            assertThatThrownBy { service.postponeGame(gameId, newScheduledAt, null) }
                .isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        @DisplayName("홈팀이 없으면 이벤트를 발행하지 않는다")
        fun doesNotPublishEventWhenHomeTeamMissing() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(awayTeam)

            // when
            service.postponeGame(gameId, newScheduledAt, null)

            // then
            verify(exactly = 0) { eventPublisher.publishEvent(any<GamePostponedEvent>()) }
        }

        @Test
        @DisplayName("원정팀이 없으면 이벤트를 발행하지 않는다")
        fun doesNotPublishEventWhenAwayTeamMissing() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(homeTeam)

            // when
            service.postponeGame(gameId, newScheduledAt, null)

            // then
            verify(exactly = 0) { eventPublisher.publishEvent(any<GamePostponedEvent>()) }
        }

        @Test
        @DisplayName("팀이 없는 경우 이벤트를 발행하지 않는다")
        fun doesNotPublishEventWhenNoTeams() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            service.postponeGame(gameId, newScheduledAt, null)

            // then
            verify(exactly = 0) { eventPublisher.publishEvent(any<GamePostponedEvent>()) }
        }
    }

    @Nested
    @DisplayName("rescheduleGame")
    inner class RescheduleGameTest {
        @Test
        @DisplayName("예정 상태의 경기 일정을 변경하고 이벤트를 발행한다")
        fun rescheduleScheduledGameSuccessfully() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(homeTeam, awayTeam)

            val eventSlot = slot<GameRescheduledEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            // when
            val result = service.rescheduleGame(gameId, newScheduledAt)

            // then
            assertThat(result).isEqualTo(game)
            verify(exactly = 1) { game.reschedule(newScheduledAt) }
            verify(exactly = 1) { gameRepository.save(game) }
            verify(exactly = 1) { eventPublisher.publishEvent(any<GameRescheduledEvent>()) }

            assertThat(eventSlot.captured.gameId).isEqualTo(gameId)
            assertThat(eventSlot.captured.homeTeamId).isEqualTo(10L)
            assertThat(eventSlot.captured.awayTeamId).isEqualTo(20L)
            assertThat(eventSlot.captured.newScheduledAt).isEqualTo(newScheduledAt)
        }

        @Test
        @DisplayName("연기 상태의 경기 일정을 변경할 수 있다")
        fun reschedulePostponedGameSuccessfully() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)
            val game = createGame(gameId, status = GameStatus.POSTPONED)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(homeTeam, awayTeam)

            // when
            val result = service.rescheduleGame(gameId, newScheduledAt)

            // then
            assertThat(result).isEqualTo(game)
            verify(exactly = 1) { game.reschedule(newScheduledAt) }
            verify(exactly = 1) { eventPublisher.publishEvent(any<GameRescheduledEvent>()) }
        }

        @Test
        @DisplayName("진행 중인 경기는 일정을 변경할 수 없다")
        fun cannotRescheduleInProgressGame() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)
            val game = createGame(gameId, status = GameStatus.IN_PROGRESS)

            every { gameRepository.findByIdOrNull(gameId) } returns game

            // when & then
            assertThatThrownBy { service.rescheduleGame(gameId, newScheduledAt) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정 또는 연기 상태의 경기만 일정을 변경할 수 있습니다")
        }

        @Test
        @DisplayName("종료된 경기는 일정을 변경할 수 없다")
        fun cannotRescheduleFinishedGame() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)
            val game = createGame(gameId, status = GameStatus.FINISHED)

            every { gameRepository.findByIdOrNull(gameId) } returns game

            // when & then
            assertThatThrownBy { service.rescheduleGame(gameId, newScheduledAt) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정 또는 연기 상태의 경기만 일정을 변경할 수 있습니다")
        }

        @Test
        @DisplayName("취소된 경기는 일정을 변경할 수 없다")
        fun cannotRescheduleCancelledGame() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)
            val game = createGame(gameId, status = GameStatus.CANCELLED)

            every { gameRepository.findByIdOrNull(gameId) } returns game

            // when & then
            assertThatThrownBy { service.rescheduleGame(gameId, newScheduledAt) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정 또는 연기 상태의 경기만 일정을 변경할 수 있습니다")
        }

        @Test
        @DisplayName("존재하지 않는 경기는 GameNotFoundException을 던진다")
        fun rescheduleNonExistentGameThrowsException() {
            // given
            val gameId = 999L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)

            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when & then
            assertThatThrownBy { service.rescheduleGame(gameId, newScheduledAt) }
                .isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        @DisplayName("홈팀이 없으면 이벤트를 발행하지 않는다")
        fun doesNotPublishEventWhenHomeTeamMissing() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(awayTeam)

            // when
            service.rescheduleGame(gameId, newScheduledAt)

            // then
            verify(exactly = 0) { eventPublisher.publishEvent(any<GameRescheduledEvent>()) }
        }

        @Test
        @DisplayName("원정팀이 없으면 이벤트를 발행하지 않는다")
        fun doesNotPublishEventWhenAwayTeamMissing() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(homeTeam)

            // when
            service.rescheduleGame(gameId, newScheduledAt)

            // then
            verify(exactly = 0) { eventPublisher.publishEvent(any<GameRescheduledEvent>()) }
        }

        @Test
        @DisplayName("팀이 없는 경우 이벤트를 발행하지 않는다")
        fun doesNotPublishEventWhenNoTeams() {
            // given
            val gameId = 1L
            val newScheduledAt = LocalDateTime.of(2026, 5, 10, 18, 0)
            val game = createGame(gameId, status = GameStatus.SCHEDULED)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameRepository.save(game) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            service.rescheduleGame(gameId, newScheduledAt)

            // then
            verify(exactly = 0) { eventPublisher.publishEvent(any<GameRescheduledEvent>()) }
        }
    }

    @Nested
    @DisplayName("advanceHalfInning")
    inner class AdvanceHalfInningTest {
        @Test
        @DisplayName("정규 이닝 초 종료 + 홈팀 리드 → 말 생략 + 경기 종료 + 투수 판정 + 이벤트 발행")
        fun homeTeamLeadsSkipBottomEndsGame() {
            // given
            val gameId = 1L
            val game = createGame(gameId, status = GameStatus.IN_PROGRESS)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)
            val gameTeams = listOf(homeTeam, awayTeam)

            every { game.nextHalfInning(gameTeams = gameTeams) } returns
                TiebreakerResult.HOME_TEAM_LEADS_SKIP_BOTTOM
            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns gameTeams
            every { gameRepository.save(game) } returns game
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { homeTeam.totalScore } returns 5
            every { awayTeam.totalScore } returns 3

            // when
            val result = service.advanceHalfInning(gameId, 999L)

            // then
            assertThat(result).isEqualTo(game)
            verify(exactly = 1) { gameRepository.save(game) }
            verify { eventPublisher.publishEvent(any<GameResultConfirmedEvent>()) }
        }

        @Test
        @DisplayName("정규 이닝 초 종료 + 동점 → 정상 말 이닝 진행")
        fun tiedScoreNormalProgression() {
            // given
            val gameId = 1L
            val game = createGame(gameId, status = GameStatus.IN_PROGRESS)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)
            val gameTeams = listOf(homeTeam, awayTeam)

            every { game.nextHalfInning(gameTeams = gameTeams) } returns TiebreakerResult.NORMAL
            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns gameTeams
            every { gameRepository.save(game) } returns game

            val capturedEvents = mutableListOf<Any>()
            every { eventPublisher.publishEvent(any()) } answers {
                capturedEvents.add(firstArg())
                Unit
            }

            // when
            val result = service.advanceHalfInning(gameId, 999L)

            // then
            assertThat(result).isEqualTo(game)
            verify(exactly = 1) { gameRepository.save(game) }
            assertThat(capturedEvents.filterIsInstance<GameResultConfirmedEvent>()).isEmpty()
        }

        @Test
        @DisplayName("정규 이닝 초 종료 + 원정 리드 → 정상 말 이닝 진행")
        fun awayTeamLeadsNormalProgression() {
            // given
            val gameId = 1L
            val game = createGame(gameId, status = GameStatus.IN_PROGRESS)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)
            val gameTeams = listOf(homeTeam, awayTeam)

            every { game.nextHalfInning(gameTeams = gameTeams) } returns TiebreakerResult.NORMAL
            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns gameTeams
            every { gameRepository.save(game) } returns game

            val capturedEvents = mutableListOf<Any>()
            every { eventPublisher.publishEvent(any()) } answers {
                capturedEvents.add(firstArg())
                Unit
            }

            // when
            val result = service.advanceHalfInning(gameId, 999L)

            // then
            assertThat(result).isEqualTo(game)
            assertThat(capturedEvents.filterIsInstance<GameResultConfirmedEvent>()).isEmpty()
        }

        @Test
        @DisplayName("진행 중이 아닌 경기는 이닝을 진행할 수 없다")
        fun cannotAdvanceHalfInningWhenNotInProgress() {
            // given
            val gameId = 1L
            val game = createGame(gameId, status = GameStatus.FINISHED)

            every { gameRepository.findByIdOrNull(gameId) } returns game

            // when & then
            assertThatThrownBy { service.advanceHalfInning(gameId, 999L) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("진행 중인 경기만 이닝을 진행할 수 있습니다")
        }

        @Test
        @DisplayName("존재하지 않는 경기는 GameNotFoundException을 던진다")
        fun advanceHalfInningNonExistentGameThrowsException() {
            // given
            val gameId = 999L

            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when & then
            assertThatThrownBy { service.advanceHalfInning(gameId, 999L) }
                .isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        @DisplayName("홈팀 리드 자동 종료 시 GameResultConfirmedEvent에 올바른 점수가 포함된다")
        fun homeTeamLeadsPublishesCorrectScoresInEvent() {
            // given
            val gameId = 1L
            val game = createGame(gameId, status = GameStatus.IN_PROGRESS)
            val homeTeam = createGameTeam(gameId, teamId = 10L, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, teamId = 20L, homeAway = HomeAway.AWAY)
            val gameTeams = listOf(homeTeam, awayTeam)

            every { game.nextHalfInning(gameTeams = gameTeams) } returns
                TiebreakerResult.HOME_TEAM_LEADS_SKIP_BOTTOM
            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns gameTeams
            every { gameRepository.save(game) } returns game
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { homeTeam.totalScore } returns 7
            every { awayTeam.totalScore } returns 2

            val eventSlot = slot<GameResultConfirmedEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            // when
            service.advanceHalfInning(gameId, 999L)

            // then
            verify { eventPublisher.publishEvent(any<GameResultConfirmedEvent>()) }
            assertThat(eventSlot.captured.gameId).isEqualTo(gameId)
            assertThat(eventSlot.captured.homeTeamId).isEqualTo(10L)
            assertThat(eventSlot.captured.awayTeamId).isEqualTo(20L)
            assertThat(eventSlot.captured.homeScore).isEqualTo(7)
            assertThat(eventSlot.captured.awayScore).isEqualTo(2)
        }
    }

    // Helper methods
    private fun createGame(
        id: Long,
        status: GameStatus = GameStatus.SCHEDULED,
    ): Game {
        val game = mockk<Game>(relaxed = true)
        every { game.id } returns id
        every { game.status } returns status
        return game
    }

    private fun createGameTeam(
        gameId: Long,
        teamId: Long = 1L,
        homeAway: HomeAway = HomeAway.HOME,
    ): GameTeam {
        val gameTeam = mockk<GameTeam>(relaxed = true)
        val game = mockk<Game>(relaxed = true)
        val team = mockk<Team>(relaxed = true)

        every { gameTeam.game } returns game
        every { gameTeam.team } returns team
        every { gameTeam.homeAway } returns homeAway

        every { game.id } returns gameId
        every { team.id } returns teamId
        every { team.name } returns "팀 $teamId"

        return gameTeam
    }
}
