package com.nextup.infrastructure.repository.stats

import com.nextup.core.domain.stats.SeasonPitchingStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SeasonPitchingStatsRepository : JpaRepository<SeasonPitchingStats, Long> {

    /**
     * 선수 ID와 연도로 시즌 투수 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonPitchingStats s WHERE s.player.id = :playerId AND s.year = :year")
    fun findByPlayerIdAndYear(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int
    ): SeasonPitchingStats?

    /**
     * 선수 ID로 모든 시즌 투수 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonPitchingStats s WHERE s.player.id = :playerId ORDER BY s.year DESC")
    fun findAllByPlayerId(@Param("playerId") playerId: Long): List<SeasonPitchingStats>

    /**
     * 특정 연도의 모든 시즌 투수 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonPitchingStats s WHERE s.year = :year")
    fun findAllByYear(@Param("year") year: Int): List<SeasonPitchingStats>

    /**
     * ERA 상위 N명을 조회합니다 (최소 이닝 조건).
     * ERA는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    @Query("""
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        AND s.inningsPitchedOuts >= :minInningsPitchedOuts
        ORDER BY (CAST(s.earnedRuns AS double) * 27.0 / CAST(s.inningsPitchedOuts AS double)) ASC
        LIMIT :limit
    """)
    fun findTopByEra(
        @Param("year") year: Int,
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int
    ): List<SeasonPitchingStats>

    /**
     * 승수 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        ORDER BY s.wins DESC
        LIMIT :limit
    """)
    fun findTopByWins(
        @Param("year") year: Int,
        @Param("limit") limit: Int
    ): List<SeasonPitchingStats>

    /**
     * 삼진 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        ORDER BY s.strikeouts DESC
        LIMIT :limit
    """)
    fun findTopByStrikeouts(
        @Param("year") year: Int,
        @Param("limit") limit: Int
    ): List<SeasonPitchingStats>

    /**
     * 세이브 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        ORDER BY s.saves DESC
        LIMIT :limit
    """)
    fun findTopBySaves(
        @Param("year") year: Int,
        @Param("limit") limit: Int
    ): List<SeasonPitchingStats>

    /**
     * WHIP 상위 N명을 조회합니다 (최소 이닝 조건).
     * WHIP는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    @Query("""
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        AND s.inningsPitchedOuts >= :minInningsPitchedOuts
        ORDER BY (CAST(s.hitsAllowed + s.walksAllowed AS double) * 3.0 / CAST(s.inningsPitchedOuts AS double)) ASC
        LIMIT :limit
    """)
    fun findTopByWhip(
        @Param("year") year: Int,
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int
    ): List<SeasonPitchingStats>
}
