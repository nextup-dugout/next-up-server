package com.nextup.infrastructure.service.player

import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.PlayerTeamHistoryRepositoryPort
import com.nextup.core.service.player.PlayerDashboardService
import com.nextup.core.service.player.dto.PlayerDashboardDto
import com.nextup.core.service.stats.PlayerStatsService
import com.nextup.core.service.stats.RecentFormService
import com.nextup.core.service.stats.dto.FormType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 선수 대시보드 통합 서비스 구현체
 *
 * 기존 서비스들을 조합하여 선수 프로필 화면에 필요한 모든 데이터를 한 번에 수집합니다.
 * N+1 방지를 위해 각 데이터를 배치로 조회합니다.
 */
@Service
@Transactional(readOnly = true)
class PlayerDashboardServiceImpl(
    private val playerRepository: PlayerRepositoryPort,
    private val playerTeamHistoryRepository: PlayerTeamHistoryRepositoryPort,
    private val playerStatsService: PlayerStatsService,
    private val recentFormService: RecentFormService,
) : PlayerDashboardService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getPlayerDashboard(playerId: Long): PlayerDashboardDto {
        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw PlayerNotFoundException(playerId)

        val currentYear = LocalDate.now().year

        // 현재 소속 이력 조회
        val currentHistory = playerTeamHistoryRepository.findActiveByPlayerId(playerId).firstOrNull()

        // 전체 소속 이력 조회 (팀 이력 탭)
        val teamHistory = playerTeamHistoryRepository.findByPlayerIdWithDetails(playerId)

        // 시즌 타격 통계 (없으면 null)
        val seasonBattingStats =
            try {
                playerStatsService.getSeasonBattingStats(playerId, currentYear)
            } catch (e: IllegalArgumentException) {
                log.debug("시즌 타격 통계 없음: playerId={}, year={}", playerId, currentYear)
                null
            }

        // 시즌 투수 통계 (없으면 null)
        val seasonPitchingStats =
            try {
                playerStatsService.getSeasonPitchingStats(playerId, currentYear)
            } catch (e: IllegalArgumentException) {
                log.debug("시즌 투수 통계 없음: playerId={}, year={}", playerId, currentYear)
                null
            }

        // 통산 타격 통계 (없으면 null)
        val careerBattingStats =
            try {
                playerStatsService.getCareerBattingStats(playerId)
            } catch (e: IllegalArgumentException) {
                log.debug("통산 타격 통계 없음: playerId={}", playerId)
                null
            }

        // 통산 투수 통계 (없으면 null)
        val careerPitchingStats =
            try {
                playerStatsService.getCareerPitchingStats(playerId)
            } catch (e: IllegalArgumentException) {
                log.debug("통산 투수 통계 없음: playerId={}", playerId)
                null
            }

        // 최근 타격 폼 (없으면 null)
        val recentBattingForm =
            try {
                recentFormService.getRecentForm(playerId, RECENT_FORM_GAMES, FormType.BATTING)
            } catch (e: Exception) {
                log.debug("최근 타격 폼 없음: playerId={}, message={}", playerId, e.message)
                null
            }

        // 최근 투수 폼 (없으면 null)
        val recentPitchingForm =
            try {
                recentFormService.getRecentForm(playerId, RECENT_FORM_GAMES, FormType.PITCHING)
            } catch (e: Exception) {
                log.debug("최근 투수 폼 없음: playerId={}, message={}", playerId, e.message)
                null
            }

        return PlayerDashboardDto(
            player = player,
            currentHistory = currentHistory,
            seasonBattingStats = seasonBattingStats,
            seasonPitchingStats = seasonPitchingStats,
            careerBattingStats = careerBattingStats,
            careerPitchingStats = careerPitchingStats,
            recentBattingForm = recentBattingForm,
            recentPitchingForm = recentPitchingForm,
            teamHistory = teamHistory,
        )
    }

    companion object {
        private const val RECENT_FORM_GAMES = 5
    }
}
