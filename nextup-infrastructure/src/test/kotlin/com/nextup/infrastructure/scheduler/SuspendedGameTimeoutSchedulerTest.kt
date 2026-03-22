package com.nextup.infrastructure.scheduler

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.port.repository.GameRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SuspendedGameTimeoutScheduler 테스트")
class SuspendedGameTimeoutSchedulerTest {
    private val gameRepository = mockk<GameRepositoryPort>()

    private val scheduler =
        SuspendedGameTimeoutScheduler(
            gameRepository = gameRepository,
        )

    @Nested
    @DisplayName("SUSPENDED 경기 타임아웃 처리")
    inner class CancelTimedOutSuspendedGames {
        @Test
        fun `SUSPENDED 경기가 없으면 아무 작업도 하지 않는다`() {
            // given
            every { gameRepository.findByStatus(GameStatus.SUSPENDED) } returns emptyList()

            // when
            scheduler.cancelTimedOutSuspendedGames()

            // then
            verify(exactly = 0) { gameRepository.save(any()) }
        }

        @Test
        fun `타임아웃된 SUSPENDED 경기를 자동 취소한다`() {
            // given
            val timedOutGame = mockk<Game>(relaxed = true)
            every { timedOutGame.id } returns 1L
            every { timedOutGame.isSuspendedTimeout(any(), any()) } returns true

            every { gameRepository.findByStatus(GameStatus.SUSPENDED) } returns listOf(timedOutGame)
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            scheduler.cancelTimedOutSuspendedGames()

            // then
            verify(exactly = 1) { timedOutGame.cancelByTimeout() }
            verify(exactly = 1) { gameRepository.save(timedOutGame) }
        }

        @Test
        fun `타임아웃되지 않은 SUSPENDED 경기는 취소하지 않는다`() {
            // given
            val recentGame = mockk<Game>(relaxed = true)
            every { recentGame.id } returns 1L
            every { recentGame.isSuspendedTimeout(any(), any()) } returns false

            every { gameRepository.findByStatus(GameStatus.SUSPENDED) } returns listOf(recentGame)

            // when
            scheduler.cancelTimedOutSuspendedGames()

            // then
            verify(exactly = 0) { recentGame.cancelByTimeout() }
            verify(exactly = 0) { gameRepository.save(any()) }
        }

        @Test
        fun `여러 SUSPENDED 경기 중 타임아웃된 것만 취소한다`() {
            // given
            val timedOutGame = mockk<Game>(relaxed = true)
            every { timedOutGame.id } returns 1L
            every { timedOutGame.isSuspendedTimeout(any(), any()) } returns true

            val recentGame = mockk<Game>(relaxed = true)
            every { recentGame.id } returns 2L
            every { recentGame.isSuspendedTimeout(any(), any()) } returns false

            every {
                gameRepository.findByStatus(GameStatus.SUSPENDED)
            } returns listOf(timedOutGame, recentGame)
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            scheduler.cancelTimedOutSuspendedGames()

            // then
            verify(exactly = 1) { timedOutGame.cancelByTimeout() }
            verify(exactly = 0) { recentGame.cancelByTimeout() }
            verify(exactly = 1) { gameRepository.save(timedOutGame) }
        }
    }
}
