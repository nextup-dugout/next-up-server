package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PitchingRecordRepository :
    JpaRepository<PitchingRecord, Long>,
    PitchingRecordRepositoryPort {
    /**
     * GamePlayer로 투수 기록을 조회합니다.
     */
    override fun findByGamePlayer(gamePlayer: GamePlayer): PitchingRecord?

    /**
     * GamePlayer ID로 투수 기록을 조회합니다.
     */
    @Query("SELECT pr FROM PitchingRecord pr WHERE pr.gamePlayer.id = :gamePlayerId")
    override fun findByGamePlayerId(
        @Param("gamePlayerId") gamePlayerId: Long,
    ): PitchingRecord?

    /**
     * 경기 ID로 모든 투수 기록을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
    """,
    )
    override fun findAllByGameId(
        @Param("gameId") gameId: Long,
    ): List<PitchingRecord>

    /**
     * 선수 ID로 모든 투수 기록을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.player.id = :playerId
        ORDER BY gp.game.scheduledDate DESC
    """,
    )
    override fun findAllByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<PitchingRecord>

    /**
     * 선수의 최근 N경기 투수 기록을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.player.id = :playerId
        ORDER BY gp.game.scheduledDate DESC
        LIMIT :limit
    """,
    )
    override fun findRecentByPlayerId(
        @Param("playerId") playerId: Long,
        @Param("limit") limit: Int,
    ): List<PitchingRecord>

    /**
     * 팀의 특정 경기 투수 기록을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        JOIN gp.gameTeam gt
        WHERE gt.team.id = :teamId
        AND gp.game.id = :gameId
    """,
    )
    override fun findAllByTeamIdAndGameId(
        @Param("teamId") teamId: Long,
        @Param("gameId") gameId: Long,
    ): List<PitchingRecord>

    /**
     * 경기의 선발 투수 기록을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.isStartingPitcher = true
    """,
    )
    override fun findStartingPitchersByGameId(
        @Param("gameId") gameId: Long,
    ): List<PitchingRecord>

    /**
     * 경기의 구원 투수 기록을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.isStartingPitcher = false
    """,
    )
    override fun findReliefPitchersByGameId(
        @Param("gameId") gameId: Long,
    ): List<PitchingRecord>

    /**
     * 특정 결정(WIN/LOSS/SAVE/HOLD)을 받은 투수 기록을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        WHERE pr.decision = :decision
    """,
    )
    override fun findAllByDecision(
        @Param("decision") decision: PitchingDecision,
    ): List<PitchingRecord>

    /**
     * 경기의 승리 투수를 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.decision = 'WIN'
    """,
    )
    override fun findWinningPitcherByGameId(
        @Param("gameId") gameId: Long,
    ): PitchingRecord?

    /**
     * 경기의 패전 투수를 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.decision = 'LOSS'
    """,
    )
    override fun findLosingPitcherByGameId(
        @Param("gameId") gameId: Long,
    ): PitchingRecord?

    /**
     * 경기의 세이브 투수를 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.decision = 'SAVE'
    """,
    )
    override fun findSavePitcherByGameId(
        @Param("gameId") gameId: Long,
    ): PitchingRecord?

    /**
     * ERA 상위 N명을 조회합니다 (최소 이닝 조건).
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        WHERE pr.inningsPitchedOuts >= :minInningsPitchedOuts
        ORDER BY (CAST(pr.earnedRuns AS double) * 27.0 / CAST(pr.inningsPitchedOuts AS double)) ASC
        LIMIT :limit
    """,
    )
    override fun findTopByEarnedRunAverage(
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int,
    ): List<PitchingRecord>

    /**
     * 삼진 상위 N명을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        ORDER BY pr.strikeouts DESC
        LIMIT :limit
    """,
    )
    override fun findTopByStrikeouts(
        @Param("limit") limit: Int,
    ): List<PitchingRecord>

    /**
     * 승수 상위 N명을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        WHERE pr.decision = 'WIN'
        ORDER BY pr.id DESC
        LIMIT :limit
    """,
    )
    override fun findTopByWins(
        @Param("limit") limit: Int,
    ): List<PitchingRecord>

    /**
     * 세이브 상위 N명을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        WHERE pr.decision = 'SAVE'
        ORDER BY pr.id DESC
        LIMIT :limit
    """,
    )
    override fun findTopBySaves(
        @Param("limit") limit: Int,
    ): List<PitchingRecord>

    /**
     * 선수의 특정 연도 투수 기록을 조회합니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.player.id = :playerId
        AND FUNCTION('YEAR', gp.game.scheduledAt) = :year
        ORDER BY gp.game.scheduledAt DESC
    """,
    )
    override fun findAllByPlayerIdAndYear(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
    ): List<PitchingRecord>

    /**
     * 팀의 여러 경기 투수 기록을 한 번에 조회합니다. (N+1 방지용 배치 쿼리)
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        JOIN gp.gameTeam gt
        WHERE gt.team.id = :teamId
        AND gp.game.id IN :gameIds
    """,
    )
    override fun findAllByTeamIdAndGameIds(
        @Param("teamId") teamId: Long,
        @Param("gameIds") gameIds: List<Long>,
    ): List<PitchingRecord>

    /**
     * 선수 ID로 모든 투수 기록을 조회합니다. (FETCH JOIN으로 N+1 방지)
     * gamePlayer, gameTeam, game을 함께 로딩하여 커리어 통계 계산 시 추가 쿼리가 발생하지 않습니다.
     */
    @Query(
        """
        SELECT pr FROM PitchingRecord pr
        JOIN FETCH pr.gamePlayer gp
        JOIN FETCH gp.gameTeam gt
        JOIN FETCH gt.game g
        WHERE gp.player.id = :playerId
        ORDER BY g.scheduledAt DESC
    """,
    )
    override fun findAllByPlayerIdWithGameInfo(
        @Param("playerId") playerId: Long,
    ): List<PitchingRecord>
}
