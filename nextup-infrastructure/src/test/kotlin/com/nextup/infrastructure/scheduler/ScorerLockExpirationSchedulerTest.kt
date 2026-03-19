package com.nextup.infrastructure.scheduler

import com.nextup.core.domain.game.Game
import com.nextup.core.port.repository.GameRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("ScorerLockExpirationScheduler 테스트")
class ScorerLockExpirationSchedulerTest {
    private val gameRepository = mockk<GameRepositoryPort>()

    private val scheduler =
        ScorerLockExpirationScheduler(
            gameRepository = gameRepository,
        )

    @Nested
    @DisplayName("잠금 만료 처리")
    inner class ExpireLockedGames {
        @Test
        fun `만료된 잠금이 자동으로 해제된다`() {
            // given
            val expiredGame = mockk<Game>(relaxed = true)
            every { expiredGame.id } returns 1L
            every { expiredGame.scorerId } returns 100L
            every { expiredGame.lockedAt } returns LocalDateTime.now().minusMinutes(31)
            every {
                gameRepository.findLockedGamesBefore(any())
            } returns listOf(expiredGame)
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            scheduler.expireLockedGames()

            // then
            verify(exactly = 1) { expiredGame.expireLock() }
            verify(exactly = 1) { gameRepository.save(expiredGame) }
        }

        @Test
        fun `만료 대상이 없으면 아무 작업도 하지 않는다`() {
            // given
            every {
                gameRepository.findLockedGamesBefore(any())
            } returns emptyList()

            // when
            scheduler.expireLockedGames()

            // then
            verify(exactly = 0) { gameRepository.save(any()) }
        }

        @Test
        fun `여러 만료 잠금이 모두 처리된다`() {
            // given
            val game1 = mockk<Game>(relaxed = true)
            every { game1.id } returns 1L
            every { game1.scorerId } returns 100L
            every { game1.lockedAt } returns LocalDateTime.now().minusMinutes(35)

            val game2 = mockk<Game>(relaxed = true)
            every { game2.id } returns 2L
            every { game2.scorerId } returns 200L
            every { game2.lockedAt } returns LocalDateTime.now().minusMinutes(60)

            every {
                gameRepository.findLockedGamesBefore(any())
            } returns listOf(game1, game2)
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            scheduler.expireLockedGames()

            // then
            verify(exactly = 1) { game1.expireLock() }
            verify(exactly = 1) { game2.expireLock() }
            verify(exactly = 2) { gameRepository.save(any()) }
        }
    }
}
