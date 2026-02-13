package com.nextup.infrastructure.repository.stats

import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SeasonBattingStatsRepository :
    JpaRepository<SeasonBattingStats, Long>,
    SeasonBattingStatsRepositoryPort {
    /**
     * 선수 ID와 연도로 시즌 타격 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonBattingStats s WHERE s.player.id = :playerId AND s.year = :year")
    override fun findByPlayerIdAndYear(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
    ): SeasonBattingStats?

    /**
     * 선수 ID로 모든 시즌 타격 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonBattingStats s WHERE s.player.id = :playerId ORDER BY s.year DESC")
    override fun findAllByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<SeasonBattingStats>

    /**
     * 특정 연도의 모든 시즌 타격 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonBattingStats s WHERE s.year = :year")
    override fun findAllByYear(
        @Param("year") year: Int,
    ): List<SeasonBattingStats>

    /**
     * 타율 상위 N명을 조회합니다 (최소 타수 조건).
     */
    @Query(
        """
        SELECT s FROM SeasonBattingStats s
        WHERE s.year = :year
        AND s.atBats >= :minAtBats
        ORDER BY (CAST(s.hits AS double) / CAST(s.atBats AS double)) DESC
        LIMIT :limit
    """,
    )
    override fun findTopByBattingAverage(
        @Param("year") year: Int,
        @Param("minAtBats") minAtBats: Int,
        @Param("limit") limit: Int,
    ): List<SeasonBattingStats>

    /**
     * 홈런 상위 N명을 조회합니다.
     */
    @Query(
        """
        SELECT s FROM SeasonBattingStats s
        WHERE s.year = :year
        ORDER BY s.homeRuns DESC
        LIMIT :limit
    """,
    )
    override fun findTopByHomeRuns(
        @Param("year") year: Int,
        @Param("limit") limit: Int,
    ): List<SeasonBattingStats>

    /**
     * 타점 상위 N명을 조회합니다.
     */
    @Query(
        """
        SELECT s FROM SeasonBattingStats s
        WHERE s.year = :year
        ORDER BY s.runsBattedIn DESC
        LIMIT :limit
    """,
    )
    override fun findTopByRunsBattedIn(
        @Param("year") year: Int,
        @Param("limit") limit: Int,
    ): List<SeasonBattingStats>

    /**
     * OPS 상위 N명을 조회합니다 (최소 타석 조건).
     */
    @Query(
        """
        SELECT s FROM SeasonBattingStats s
        WHERE s.year = :year
        AND s.plateAppearances >= :minPlateAppearances
        ORDER BY (
            (CAST(s.hits + s.walks + s.intentionalWalks + s.hitByPitch AS double) /
             CAST(s.atBats + s.walks + s.intentionalWalks + s.hitByPitch + s.sacrificeFlies AS double)) +
            (CAST(s.hits - s.doubles - s.triples - s.homeRuns +
                  (2 * s.doubles) + (3 * s.triples) + (4 * s.homeRuns) AS double) /
             CAST(s.atBats AS double))
        ) DESC
        LIMIT :limit
    """,
    )
    override fun findTopByOps(
        @Param("year") year: Int,
        @Param("minPlateAppearances") minPlateAppearances: Int,
        @Param("limit") limit: Int,
    ): List<SeasonBattingStats>

    @Query(
        """
        SELECT s FROM SeasonBattingStats s
        WHERE s.year = :year
        ORDER BY s.hits DESC
        LIMIT :limit
    """,
    )
    override fun findTopByHits(
        @Param("year") year: Int,
        @Param("limit") limit: Int,
    ): List<SeasonBattingStats>

    @Query(
        """
        SELECT s FROM SeasonBattingStats s
        WHERE s.year = :year
        ORDER BY s.stolenBases DESC
        LIMIT :limit
    """,
    )
    override fun findTopByStolenBases(
        @Param("year") year: Int,
        @Param("limit") limit: Int,
    ): List<SeasonBattingStats>

    @Query(
        """
        SELECT s FROM SeasonBattingStats s
        WHERE s.year = :year
        AND s.plateAppearances >= :minPlateAppearances
        ORDER BY (
            CAST(s.hits + s.walks + s.intentionalWalks + s.hitByPitch AS double) /
            CAST(s.atBats + s.walks + s.intentionalWalks + s.hitByPitch + s.sacrificeFlies AS double)
        ) DESC
        LIMIT :limit
    """,
    )
    override fun findTopByOnBasePercentage(
        @Param("year") year: Int,
        @Param("minPlateAppearances") minPlateAppearances: Int,
        @Param("limit") limit: Int,
    ): List<SeasonBattingStats>

    @Query(
        """
        SELECT s FROM SeasonBattingStats s
        WHERE s.year = :year
        AND s.plateAppearances >= :minPlateAppearances
        AND s.atBats > 0
        ORDER BY (
            CAST(s.hits - s.doubles - s.triples - s.homeRuns +
                 (2 * s.doubles) + (3 * s.triples) + (4 * s.homeRuns) AS double) /
            CAST(s.atBats AS double)
        ) DESC
        LIMIT :limit
    """,
    )
    override fun findTopBySlugging(
        @Param("year") year: Int,
        @Param("minPlateAppearances") minPlateAppearances: Int,
        @Param("limit") limit: Int,
    ): List<SeasonBattingStats>
}
