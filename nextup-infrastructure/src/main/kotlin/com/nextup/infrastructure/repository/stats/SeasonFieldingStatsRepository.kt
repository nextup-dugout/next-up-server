package com.nextup.infrastructure.repository.stats

import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SeasonFieldingStatsRepository :
    JpaRepository<SeasonFieldingStats, Long>,
    SeasonFieldingStatsRepositoryPort {
    /**
     * 선수 ID와 연도로 시즌 수비 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonFieldingStats s WHERE s.player.id = :playerId AND s.year = :year")
    override fun findByPlayerIdAndYear(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
    ): SeasonFieldingStats?

    /**
     * 선수 ID로 모든 시즌 수비 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonFieldingStats s WHERE s.player.id = :playerId ORDER BY s.year DESC")
    override fun findAllByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<SeasonFieldingStats>

    /**
     * 특정 연도의 모든 시즌 수비 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonFieldingStats s WHERE s.year = :year")
    override fun findAllByYear(
        @Param("year") year: Int,
    ): List<SeasonFieldingStats>
}
