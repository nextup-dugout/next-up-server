package com.nextup.infrastructure.repository.stats

import com.nextup.core.domain.stats.CareerPitchingStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CareerPitchingStatsRepository : JpaRepository<CareerPitchingStats, Long> {

    /**
     * 선수 ID로 통산 투수 통계를 조회합니다.
     */
    @Query("SELECT c FROM CareerPitchingStats c WHERE c.player.id = :playerId")
    fun findByPlayerId(@Param("playerId") playerId: Long): CareerPitchingStats?

    /**
     * 통산 ERA 상위 N명을 조회합니다 (최소 이닝 조건).
     * ERA는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    @Query("""
        SELECT c FROM CareerPitchingStats c
        WHERE c.inningsPitchedOuts >= :minInningsPitchedOuts
        ORDER BY (CAST(c.earnedRuns AS double) * 27.0 / CAST(c.inningsPitchedOuts AS double)) ASC
        LIMIT :limit
    """)
    fun findTopByEra(
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int
    ): List<CareerPitchingStats>

    /**
     * 통산 승수 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT c FROM CareerPitchingStats c
        ORDER BY c.wins DESC
        LIMIT :limit
    """)
    fun findTopByWins(@Param("limit") limit: Int): List<CareerPitchingStats>

    /**
     * 통산 삼진 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT c FROM CareerPitchingStats c
        ORDER BY c.strikeouts DESC
        LIMIT :limit
    """)
    fun findTopByStrikeouts(@Param("limit") limit: Int): List<CareerPitchingStats>

    /**
     * 통산 세이브 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT c FROM CareerPitchingStats c
        ORDER BY c.saves DESC
        LIMIT :limit
    """)
    fun findTopBySaves(@Param("limit") limit: Int): List<CareerPitchingStats>

    /**
     * 통산 WHIP 상위 N명을 조회합니다 (최소 이닝 조건).
     * WHIP는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    @Query("""
        SELECT c FROM CareerPitchingStats c
        WHERE c.inningsPitchedOuts >= :minInningsPitchedOuts
        ORDER BY (CAST(c.hitsAllowed + c.walksAllowed AS double) * 3.0 / CAST(c.inningsPitchedOuts AS double)) ASC
        LIMIT :limit
    """)
    fun findTopByWhip(
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int
    ): List<CareerPitchingStats>

    /**
     * 통산 이닝 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT c FROM CareerPitchingStats c
        ORDER BY c.inningsPitchedOuts DESC
        LIMIT :limit
    """)
    fun findTopByInningsPitched(@Param("limit") limit: Int): List<CareerPitchingStats>

    /**
     * 통산 삼진/볼넷 비율 상위 N명을 조회합니다 (최소 이닝 및 볼넷 조건).
     */
    @Query("""
        SELECT c FROM CareerPitchingStats c
        WHERE c.inningsPitchedOuts >= :minInningsPitchedOuts
        AND c.walksAllowed > 0
        ORDER BY (CAST(c.strikeouts AS double) / CAST(c.walksAllowed AS double)) DESC
        LIMIT :limit
    """)
    fun findTopByStrikeoutToWalkRatio(
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int
    ): List<CareerPitchingStats>
}
