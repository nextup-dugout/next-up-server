package com.nextup.core.service.stats

import com.nextup.common.exception.CareerFieldingStatsNotFoundException
import com.nextup.common.exception.SeasonFieldingStatsNotFoundException
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("PlayerFieldingStatsService 테스트")
class PlayerFieldingStatsServiceTest {
    private lateinit var seasonFieldingStatsRepository: SeasonFieldingStatsRepositoryPort
    private lateinit var careerFieldingStatsRepository: CareerFieldingStatsRepositoryPort
    private lateinit var playerFieldingStatsService: PlayerFieldingStatsService

    @BeforeEach
    fun setUp() {
        seasonFieldingStatsRepository = mockk()
        careerFieldingStatsRepository = mockk()
        playerFieldingStatsService =
            PlayerFieldingStatsService(
                seasonFieldingStatsRepository = seasonFieldingStatsRepository,
                careerFieldingStatsRepository = careerFieldingStatsRepository,
            )
    }

    @Nested
    @DisplayName("시즌 수비 통계 조회")
    inner class GetSeasonFieldingStatsTest {
        @Test
        fun `시즌 수비 통계를 정상적으로 조회한다`() {
            // given
            val playerId = 1L
            val year = 2026
            val mockStats = mockk<SeasonFieldingStats>(relaxed = true)
            every { seasonFieldingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns mockStats

            // when
            val result = playerFieldingStatsService.getSeasonFieldingStats(playerId, year)

            // then
            assertThat(result).isEqualTo(mockStats)
        }

        @Test
        fun `시즌 수비 통계가 없으면 SeasonFieldingStatsNotFoundException이 발생한다`() {
            // given
            val playerId = 1L
            val year = 2026
            every { seasonFieldingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns null

            // when & then
            assertThrows<SeasonFieldingStatsNotFoundException> {
                playerFieldingStatsService.getSeasonFieldingStats(playerId, year)
            }
        }
    }

    @Nested
    @DisplayName("통산 수비 통계 조회")
    inner class GetCareerFieldingStatsTest {
        @Test
        fun `통산 수비 통계를 정상적으로 조회한다`() {
            // given
            val playerId = 1L
            val mockStats = mockk<CareerFieldingStats>(relaxed = true)
            every { careerFieldingStatsRepository.findByPlayerId(playerId) } returns mockStats

            // when
            val result = playerFieldingStatsService.getCareerFieldingStats(playerId)

            // then
            assertThat(result).isEqualTo(mockStats)
        }

        @Test
        fun `통산 수비 통계가 없으면 CareerFieldingStatsNotFoundException이 발생한다`() {
            // given
            val playerId = 1L
            every { careerFieldingStatsRepository.findByPlayerId(playerId) } returns null

            // when & then
            assertThrows<CareerFieldingStatsNotFoundException> {
                playerFieldingStatsService.getCareerFieldingStats(playerId)
            }
        }
    }

    @Nested
    @DisplayName("모든 시즌 수비 통계 조회")
    inner class GetAllSeasonFieldingStatsTest {
        @Test
        fun `모든 시즌 수비 통계를 정상적으로 조회한다`() {
            // given
            val playerId = 1L
            val mockStats1 = mockk<SeasonFieldingStats>(relaxed = true)
            val mockStats2 = mockk<SeasonFieldingStats>(relaxed = true)
            every { seasonFieldingStatsRepository.findAllByPlayerId(playerId) } returns
                listOf(mockStats1, mockStats2)

            // when
            val result = playerFieldingStatsService.getAllSeasonFieldingStats(playerId)

            // then
            assertThat(result).hasSize(2)
        }

        @Test
        fun `시즌 수비 통계가 없으면 빈 리스트를 반환한다`() {
            // given
            val playerId = 99L
            every { seasonFieldingStatsRepository.findAllByPlayerId(playerId) } returns emptyList()

            // when
            val result = playerFieldingStatsService.getAllSeasonFieldingStats(playerId)

            // then
            assertThat(result).isEmpty()
        }
    }
}
