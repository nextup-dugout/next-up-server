package com.nextup.core.service.stats

import com.nextup.common.exception.CareerFieldingStatsNotFoundException
import com.nextup.common.exception.SeasonFieldingStatsNotFoundException
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 선수 수비 통계 서비스
 *
 * 선수의 시즌/통산 수비 통계를 조회합니다.
 */
@Service
@Transactional(readOnly = true)
class PlayerFieldingStatsService(
    private val seasonFieldingStatsRepository: SeasonFieldingStatsRepositoryPort,
    private val careerFieldingStatsRepository: CareerFieldingStatsRepositoryPort,
) {
    /**
     * 시즌 수비 통계를 조회합니다.
     */
    fun getSeasonFieldingStats(
        playerId: Long,
        year: Int,
    ): SeasonFieldingStats =
        seasonFieldingStatsRepository.findByPlayerIdAndYear(playerId, year)
            ?: throw SeasonFieldingStatsNotFoundException(playerId, year)

    /**
     * 통산 수비 통계를 조회합니다.
     */
    fun getCareerFieldingStats(playerId: Long): CareerFieldingStats =
        careerFieldingStatsRepository.findByPlayerId(playerId)
            ?: throw CareerFieldingStatsNotFoundException(playerId)

    /**
     * 선수의 모든 시즌 수비 통계를 조회합니다.
     */
    fun getAllSeasonFieldingStats(playerId: Long): List<SeasonFieldingStats> =
        seasonFieldingStatsRepository.findAllByPlayerId(playerId)
}
