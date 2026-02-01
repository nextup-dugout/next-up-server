package com.nextup.infrastructure.repository.stats

import com.nextup.core.domain.stats.CareerBattingStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CareerBattingStatsRepository : JpaRepository<CareerBattingStats, Long> {

    /**
     * 선수 ID로 통산 타격 통계를 조회합니다.
     */
    @Query("SELECT c FROM CareerBattingStats c WHERE c.player.id = :playerId")
    fun findByPlayerId(@Param("playerId") playerId: Long): CareerBattingStats?

    /**
     * 통산 타율 상위 N명을 조회합니다 (최소 타수 조건).
     */
    @Query("""
        SELECT c FROM CareerBattingStats c
        WHERE c.atBats >= :minAtBats
        ORDER BY (CAST(c.hits AS double) / CAST(c.atBats AS double)) DESC
        LIMIT :limit
    """)
    fun findTopByBattingAverage(
        @Param("minAtBats") minAtBats: Int,
        @Param("limit") limit: Int
    ): List<CareerBattingStats>

    /**
     * 통산 홈런 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT c FROM CareerBattingStats c
        ORDER BY c.homeRuns DESC
        LIMIT :limit
    """)
    fun findTopByHomeRuns(@Param("limit") limit: Int): List<CareerBattingStats>

    /**
     * 통산 타점 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT c FROM CareerBattingStats c
        ORDER BY c.runsBattedIn DESC
        LIMIT :limit
    """)
    fun findTopByRunsBattedIn(@Param("limit") limit: Int): List<CareerBattingStats>

    /**
     * 통산 안타 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT c FROM CareerBattingStats c
        ORDER BY c.hits DESC
        LIMIT :limit
    """)
    fun findTopByHits(@Param("limit") limit: Int): List<CareerBattingStats>

    /**
     * 통산 도루 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT c FROM CareerBattingStats c
        ORDER BY c.stolenBases DESC
        LIMIT :limit
    """)
    fun findTopByStolenBases(@Param("limit") limit: Int): List<CareerBattingStats>

    /**
     * 통산 OPS 상위 N명을 조회합니다 (최소 타석 조건).
     */
    @Query("""
        SELECT c FROM CareerBattingStats c
        WHERE c.plateAppearances >= :minPlateAppearances
        ORDER BY (
            (CAST(c.hits + c.walks + c.intentionalWalks + c.hitByPitch AS double) /
             CAST(c.atBats + c.walks + c.intentionalWalks + c.hitByPitch + c.sacrificeFlies AS double)) +
            (CAST(c.hits - c.doubles - c.triples - c.homeRuns +
                  (2 * c.doubles) + (3 * c.triples) + (4 * c.homeRuns) AS double) /
             CAST(c.atBats AS double))
        ) DESC
        LIMIT :limit
    """)
    fun findTopByOps(
        @Param("minPlateAppearances") minPlateAppearances: Int,
        @Param("limit") limit: Int
    ): List<CareerBattingStats>

    /**
     * 통산 장타율 상위 N명을 조회합니다 (최소 타수 조건).
     */
    @Query("""
        SELECT c FROM CareerBattingStats c
        WHERE c.atBats >= :minAtBats
        ORDER BY (
            CAST(c.hits - c.doubles - c.triples - c.homeRuns +
                 (2 * c.doubles) + (3 * c.triples) + (4 * c.homeRuns) AS double) /
            CAST(c.atBats AS double)
        ) DESC
        LIMIT :limit
    """)
    fun findTopBySluggingPercentage(
        @Param("minAtBats") minAtBats: Int,
        @Param("limit") limit: Int
    ): List<CareerBattingStats>
}
