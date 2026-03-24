package com.nextup.infrastructure.repository.stats

import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SeasonPitchingStatsRepository :
    JpaRepository<SeasonPitchingStats, Long>,
    SeasonPitchingStatsRepositoryPort {
    /**
     * 선수 ID와 연도로 시즌 투수 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonPitchingStats s WHERE s.player.id = :playerId AND s.year = :year")
    override fun findByPlayerIdAndYear(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
    ): SeasonPitchingStats?

    /**
     * 선수 ID, 연도, 팀 ID로 시즌 투수 통계를 조회합니다 (이적 시 팀별 기록 분리).
     */
    @Query(
        """
        SELECT s FROM SeasonPitchingStats s
        WHERE s.player.id = :playerId AND s.year = :year
        AND (s.teamId = :teamId OR (s.teamId IS NULL AND :teamId IS NULL))
    """,
    )
    override fun findByPlayerIdAndYearAndTeamId(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
        @Param("teamId") teamId: Long?,
    ): SeasonPitchingStats?

    /**
     * 선수 ID, 연도, 팀 ID, 대회 유형으로 시즌 투수 통계를 조회합니다.
     */
    @Query(
        """
        SELECT s FROM SeasonPitchingStats s
        WHERE s.player.id = :playerId AND s.year = :year
        AND (s.teamId = :teamId OR (s.teamId IS NULL AND :teamId IS NULL))
        AND s.competitionType = :competitionType
    """,
    )
    override fun findByPlayerIdAndYearAndTeamIdAndCompetitionType(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
        @Param("teamId") teamId: Long?,
        @Param("competitionType") competitionType: CompetitionType,
    ): SeasonPitchingStats?

    /**
     * 선수 ID와 연도로 모든 팀별 시즌 투수 통계를 조회합니다 (이적 시 팀별 기록).
     */
    @Query(
        """
        SELECT s FROM SeasonPitchingStats s
        WHERE s.player.id = :playerId AND s.year = :year
        ORDER BY s.teamId ASC NULLS FIRST
    """,
    )
    override fun findAllByPlayerIdAndYear(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
    ): List<SeasonPitchingStats>

    /**
     * 선수 ID로 모든 시즌 투수 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonPitchingStats s WHERE s.player.id = :playerId ORDER BY s.year DESC")
    override fun findAllByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<SeasonPitchingStats>

    /**
     * 특정 연도의 모든 시즌 투수 통계를 조회합니다.
     */
    @Query("SELECT s FROM SeasonPitchingStats s WHERE s.year = :year")
    override fun findAllByYear(
        @Param("year") year: Int,
    ): List<SeasonPitchingStats>

    /**
     * ERA 상위 N명을 조회합니다 (최소 이닝 조건, FRIENDLY 제외).
     * ERA는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    @Query(
        """
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        AND s.inningsPitchedOuts >= :minInningsPitchedOuts
        AND s.competitionType <> 'FRIENDLY'
        ORDER BY (CAST(s.earnedRuns AS double) * 27.0 / CAST(s.inningsPitchedOuts AS double)) ASC
        LIMIT :limit
    """,
    )
    override fun findTopByEra(
        @Param("year") year: Int,
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * 승수 상위 N명을 조회합니다 (FRIENDLY 제외).
     */
    @Query(
        """
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        AND s.competitionType <> 'FRIENDLY'
        ORDER BY s.wins DESC
        LIMIT :limit
    """,
    )
    override fun findTopByWins(
        @Param("year") year: Int,
        @Param("limit") limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * 삼진 상위 N명을 조회합니다 (FRIENDLY 제외).
     */
    @Query(
        """
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        AND s.competitionType <> 'FRIENDLY'
        ORDER BY s.strikeouts DESC
        LIMIT :limit
    """,
    )
    override fun findTopByStrikeouts(
        @Param("year") year: Int,
        @Param("limit") limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * 세이브 상위 N명을 조회합니다 (FRIENDLY 제외).
     */
    @Query(
        """
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        AND s.competitionType <> 'FRIENDLY'
        ORDER BY s.saves DESC
        LIMIT :limit
    """,
    )
    override fun findTopBySaves(
        @Param("year") year: Int,
        @Param("limit") limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * WHIP 상위 N명을 조회합니다 (최소 이닝 조건, FRIENDLY 제외).
     * WHIP는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    @Query(
        """
        SELECT s FROM SeasonPitchingStats s
        WHERE s.year = :year
        AND s.inningsPitchedOuts >= :minInningsPitchedOuts
        AND s.competitionType <> 'FRIENDLY'
        ORDER BY (CAST(s.hitsAllowed + s.walksAllowed AS double) * 3.0 / CAST(s.inningsPitchedOuts AS double)) ASC
        LIMIT :limit
    """,
    )
    override fun findTopByWhip(
        @Param("year") year: Int,
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * 특정 경기에 등판하여 투구 기록이 있는 선수들의 시즌 투구 통계를 조회합니다.
     * 경기 연도를 기준으로 해당 선수들의 시즌 투구 통계를 반환합니다.
     */
    @Query(
        """
        SELECT DISTINCT sps FROM SeasonPitchingStats sps
        WHERE sps.player.id IN (
            SELECT pr.gamePlayer.player.id FROM PitchingRecord pr
            WHERE pr.gamePlayer.gameTeam.game.id = :gameId
        )
        AND sps.year = (
            SELECT FUNCTION('YEAR', g.scheduledAt) FROM Game g WHERE g.id = :gameId
        )
    """,
    )
    override fun findAllByGameId(
        @Param("gameId") gameId: Long,
    ): List<SeasonPitchingStats>
}
