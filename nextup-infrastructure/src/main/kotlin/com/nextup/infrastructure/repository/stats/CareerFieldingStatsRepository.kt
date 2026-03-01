package com.nextup.infrastructure.repository.stats

import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CareerFieldingStatsRepository :
    JpaRepository<CareerFieldingStats, Long>,
    CareerFieldingStatsRepositoryPort {
    /**
     * 선수 ID로 통산 수비 통계를 조회합니다.
     */
    @Query("SELECT c FROM CareerFieldingStats c WHERE c.player.id = :playerId")
    override fun findByPlayerId(
        @Param("playerId") playerId: Long,
    ): CareerFieldingStats?
}
