package com.nextup.core.port.repository

import com.nextup.core.domain.stats.SeasonFieldingStats

/**
 * SeasonFieldingStats Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface SeasonFieldingStatsRepositoryPort {
    fun save(stats: SeasonFieldingStats): SeasonFieldingStats

    /**
     * 선수 ID와 연도로 시즌 수비 통계를 조회합니다.
     */
    fun findByPlayerIdAndYear(
        playerId: Long,
        year: Int,
    ): SeasonFieldingStats?

    /**
     * 선수 ID로 모든 시즌 수비 통계를 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<SeasonFieldingStats>

    /**
     * 특정 연도의 모든 시즌 수비 통계를 조회합니다.
     */
    fun findAllByYear(year: Int): List<SeasonFieldingStats>
}
