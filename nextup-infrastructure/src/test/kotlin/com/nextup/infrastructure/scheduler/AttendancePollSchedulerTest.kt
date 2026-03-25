package com.nextup.infrastructure.scheduler

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.service.attendance.AttendanceService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("AttendancePollScheduler 테스트")
class AttendancePollSchedulerTest {
    private val attendanceService = mockk<AttendanceService>()
    private val gameRepository = mockk<GameRepositoryPort>()

    private val scheduler =
        AttendancePollScheduler(
            attendanceService = attendanceService,
            gameRepository = gameRepository,
        )

    @Nested
    @DisplayName("createPollsForUpcomingGames")
    inner class CreatePollsForUpcomingGames {
        @Test
        fun `예정된 경기가 있으면 출석 투표를 생성한다`() {
            // given
            val game1 = createMockGame(1L, GameStatus.SCHEDULED)
            val game2 = createMockGame(2L, GameStatus.SCHEDULED)

            every { gameRepository.findByScheduledAtBetween(any(), any()) } returns
                listOf(game1, game2)
            every { attendanceService.createPollsForGame(1L) } returns listOf(mockk(), mockk())
            every { attendanceService.createPollsForGame(2L) } returns listOf(mockk(), mockk())

            // when
            scheduler.createPollsForUpcomingGames()

            // then
            verify(exactly = 1) { attendanceService.createPollsForGame(1L) }
            verify(exactly = 1) { attendanceService.createPollsForGame(2L) }
        }

        @Test
        fun `예정된 경기가 없으면 아무 작업도 하지 않는다`() {
            // given
            every { gameRepository.findByScheduledAtBetween(any(), any()) } returns emptyList()

            // when
            scheduler.createPollsForUpcomingGames()

            // then
            verify(exactly = 0) { attendanceService.createPollsForGame(any()) }
        }

        @Test
        fun `SCHEDULED 상태가 아닌 경기는 무시한다`() {
            // given
            val scheduledGame = createMockGame(1L, GameStatus.SCHEDULED)
            val inProgressGame = createMockGame(2L, GameStatus.IN_PROGRESS)

            every { gameRepository.findByScheduledAtBetween(any(), any()) } returns
                listOf(scheduledGame, inProgressGame)
            every { attendanceService.createPollsForGame(1L) } returns listOf(mockk())

            // when
            scheduler.createPollsForUpcomingGames()

            // then
            verify(exactly = 1) { attendanceService.createPollsForGame(1L) }
            verify(exactly = 0) { attendanceService.createPollsForGame(2L) }
        }

        @Test
        fun `투표 생성 중 예외가 발생해도 다른 경기는 계속 처리한다`() {
            // given
            val game1 = createMockGame(1L, GameStatus.SCHEDULED)
            val game2 = createMockGame(2L, GameStatus.SCHEDULED)

            every { gameRepository.findByScheduledAtBetween(any(), any()) } returns
                listOf(game1, game2)
            every { attendanceService.createPollsForGame(1L) } throws RuntimeException("DB error")
            every { attendanceService.createPollsForGame(2L) } returns listOf(mockk())

            // when
            scheduler.createPollsForUpcomingGames()

            // then
            verify(exactly = 1) { attendanceService.createPollsForGame(1L) }
            verify(exactly = 1) { attendanceService.createPollsForGame(2L) }
        }
    }

    @Nested
    @DisplayName("closeExpiredPolls")
    inner class CloseExpiredPolls {
        @Test
        fun `만료된 투표를 마감한다`() {
            // given
            every { attendanceService.closeExpiredPolls() } returns 3

            // when
            scheduler.closeExpiredPolls()

            // then
            verify(exactly = 1) { attendanceService.closeExpiredPolls() }
        }

        @Test
        fun `만료된 투표가 없으면 0건을 처리한다`() {
            // given
            every { attendanceService.closeExpiredPolls() } returns 0

            // when
            scheduler.closeExpiredPolls()

            // then
            verify(exactly = 1) { attendanceService.closeExpiredPolls() }
        }
    }

    private fun createMockGame(
        id: Long,
        status: GameStatus,
        scheduledAt: LocalDateTime = LocalDateTime.now().plusDays(3),
    ): Game {
        val game: Game = mockk(relaxed = true)
        every { game.id } returns id
        every { game.status } returns status
        every { game.scheduledAt } returns scheduledAt
        return game
    }
}
