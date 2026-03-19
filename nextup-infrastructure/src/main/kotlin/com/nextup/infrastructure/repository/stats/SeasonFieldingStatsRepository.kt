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
     * 선수 ID, 연도, 팀 ID로 시즌 수비 통계를 조회합니다 (이적 시 팀별 기록 분리).
     */
    @Query(
        """
        SELECT s FROM SeasonFieldingStats s
        WHERE s.player.id = :playerId AND s.year = :year
        AND (s.teamId = :teamId OR (s.teamId IS NULL AND :teamId IS NULL))
    """,
    )
    override fun findByPlayerIdAndYearAndTeamId(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
        @Param("teamId") teamId: Long?,
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

    /**
     * 경기 ID로 해당 경기에 참여한 선수들의 시즌 수비 통계를 조회합니다.
     * FieldingRecord → GamePlayer → Player → SeasonFieldingStats 조인.
     */
    @Query(
        """
        SELECT DISTINCT sfs FROM SeasonFieldingStats sfs
        WHERE sfs.player.id IN (
            SELECT fr.gamePlayer.player.id FROM FieldingRecord fr
            WHERE fr.gamePlayer.gameTeam.game.id = :gameId
        )
        AND sfs.year = (
            SELECT FUNCTION('YEAR', g.scheduledAt) FROM Game g WHERE g.id = :gameId
        )
    """,
    )
    override fun findAllByGameId(
        @Param("gameId") gameId: Long,
    ): List<SeasonFieldingStats>
}
