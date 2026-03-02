package com.nextup.infrastructure.service.game

import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GameScheduleServiceImplGetGameDaysTest {
    private val gameRepository: GameRepositoryPort = mockk()
    private val gameTeamRepository: GameTeamRepositoryPort = mockk()

    private lateinit var service: GameScheduleServiceImpl

    @BeforeEach
    fun setUp() {
        service =
            GameScheduleServiceImpl(
                gameRepository,
                gameTeamRepository,
            )
    }

    @Test
    @DisplayName("getGameDaysInMonth는 리포지토리에 위임한다")
    fun getGameDaysInMonthDelegatesToRepository() {
        every {
            gameRepository.findGameDaysInMonth(2026, 3, null)
        } returns listOf(1, 5, 10, 15)

        val result = service.getGameDaysInMonth(2026, 3, null)

        assertThat(result).containsExactly(1, 5, 10, 15)
        verify { gameRepository.findGameDaysInMonth(2026, 3, null) }
    }

    @Test
    @DisplayName("팀 ID를 전달하면 해당 팀의 경기 일자만 반환한다")
    fun getGameDaysInMonthWithTeamId() {
        every {
            gameRepository.findGameDaysInMonth(2026, 3, 1L)
        } returns listOf(3, 17)

        val result = service.getGameDaysInMonth(2026, 3, 1L)

        assertThat(result).containsExactly(3, 17)
        verify { gameRepository.findGameDaysInMonth(2026, 3, 1L) }
    }
}
