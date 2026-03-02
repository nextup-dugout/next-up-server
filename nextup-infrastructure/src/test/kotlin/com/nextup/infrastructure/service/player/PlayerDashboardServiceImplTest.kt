package com.nextup.infrastructure.service.player

import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.PlayerTeamHistoryRepositoryPort
import com.nextup.core.service.stats.PlayerStatsService
import com.nextup.core.service.stats.RecentFormService
import com.nextup.core.service.stats.dto.FormType
import com.nextup.core.service.stats.dto.RecentFormDto
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PlayerDashboardServiceImpl")
class PlayerDashboardServiceImplTest {
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var playerTeamHistoryRepository: PlayerTeamHistoryRepositoryPort
    private lateinit var playerStatsService: PlayerStatsService
    private lateinit var recentFormService: RecentFormService
    private lateinit var service: PlayerDashboardServiceImpl

    @BeforeEach
    fun setUp() {
        playerRepository = mockk()
        playerTeamHistoryRepository = mockk()
        playerStatsService = mockk()
        recentFormService = mockk()
        service =
            PlayerDashboardServiceImpl(
                playerRepository,
                playerTeamHistoryRepository,
                playerStatsService,
                recentFormService,
            )
    }

    @Nested
    @DisplayName("getPlayerDashboard")
    inner class GetPlayerDashboard {
        @Test
        fun `선수가 존재하지 않으면 PlayerNotFoundException을 던진다`() {
            every { playerRepository.findByIdOrNull(1L) } returns null

            assertThatThrownBy { service.getPlayerDashboard(1L) }
                .isInstanceOf(PlayerNotFoundException::class.java)
        }

        @Test
        fun `선수 대시보드 데이터를 통합하여 반환한다`() {
            val player = mockk<Player>()
            val history = mockk<PlayerTeamHistory>()
            val seasonBatting = mockk<SeasonBattingStats>()
            val seasonPitching = mockk<SeasonPitchingStats>()
            val careerBatting = mockk<CareerBattingStats>()
            val careerPitching = mockk<CareerPitchingStats>()
            val battingForm = mockk<RecentFormDto>()
            val pitchingForm = mockk<RecentFormDto>()

            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerTeamHistoryRepository.findActiveByPlayerId(1L) } returns listOf(history)
            every { playerTeamHistoryRepository.findByPlayerIdWithDetails(1L) } returns listOf(history)
            every { playerStatsService.getSeasonBattingStats(1L, any()) } returns seasonBatting
            every { playerStatsService.getSeasonPitchingStats(1L, any()) } returns seasonPitching
            every { playerStatsService.getCareerBattingStats(1L) } returns careerBatting
            every { playerStatsService.getCareerPitchingStats(1L) } returns careerPitching
            every { recentFormService.getRecentForm(1L, 5, FormType.BATTING) } returns battingForm
            every { recentFormService.getRecentForm(1L, 5, FormType.PITCHING) } returns pitchingForm

            val result = service.getPlayerDashboard(1L)

            assertThat(result.player).isEqualTo(player)
            assertThat(result.currentHistory).isEqualTo(history)
            assertThat(result.seasonBattingStats).isEqualTo(seasonBatting)
            assertThat(result.seasonPitchingStats).isEqualTo(seasonPitching)
            assertThat(result.careerBattingStats).isEqualTo(careerBatting)
            assertThat(result.careerPitchingStats).isEqualTo(careerPitching)
            assertThat(result.recentBattingForm).isEqualTo(battingForm)
            assertThat(result.recentPitchingForm).isEqualTo(pitchingForm)
            assertThat(result.teamHistory).containsExactly(history)
        }

        @Test
        fun `통계가 없으면 null로 처리한다`() {
            val player = mockk<Player>()

            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerTeamHistoryRepository.findActiveByPlayerId(1L) } returns emptyList()
            every { playerTeamHistoryRepository.findByPlayerIdWithDetails(1L) } returns emptyList()
            every { playerStatsService.getSeasonBattingStats(1L, any()) } throws IllegalArgumentException("없음")
            every { playerStatsService.getSeasonPitchingStats(1L, any()) } throws IllegalArgumentException("없음")
            every { playerStatsService.getCareerBattingStats(1L) } throws IllegalArgumentException("없음")
            every { playerStatsService.getCareerPitchingStats(1L) } throws IllegalArgumentException("없음")
            every { recentFormService.getRecentForm(1L, 5, FormType.BATTING) } throws RuntimeException("없음")
            every { recentFormService.getRecentForm(1L, 5, FormType.PITCHING) } throws RuntimeException("없음")

            val result = service.getPlayerDashboard(1L)

            assertThat(result.player).isEqualTo(player)
            assertThat(result.currentHistory).isNull()
            assertThat(result.seasonBattingStats).isNull()
            assertThat(result.seasonPitchingStats).isNull()
            assertThat(result.careerBattingStats).isNull()
            assertThat(result.careerPitchingStats).isNull()
            assertThat(result.recentBattingForm).isNull()
            assertThat(result.recentPitchingForm).isNull()
            assertThat(result.teamHistory).isEmpty()
        }
    }
}
