package com.nextup.core.port.repository

import com.nextup.core.domain.stats.CareerFieldingStats

/**
 * CareerFieldingStats Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface CareerFieldingStatsRepositoryPort {
    fun save(stats: CareerFieldingStats): CareerFieldingStats

    /**
     * 선수 ID로 통산 수비 통계를 조회합니다.
     */
    fun findByPlayerId(playerId: Long): CareerFieldingStats?
}
