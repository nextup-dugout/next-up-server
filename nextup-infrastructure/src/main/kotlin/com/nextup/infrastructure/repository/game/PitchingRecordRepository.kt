package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PitchingRecordRepository : JpaRepository<PitchingRecord, Long> {

    /**
     * GamePlayer로 투수 기록을 조회합니다.
     */
    fun findByGamePlayer(gamePlayer: GamePlayer): PitchingRecord?

    /**
     * GamePlayer ID로 투수 기록을 조회합니다.
     */
    @Query("SELECT pr FROM PitchingRecord pr WHERE pr.gamePlayer.id = :gamePlayerId")
    fun findByGamePlayerId(@Param("gamePlayerId") gamePlayerId: Long): PitchingRecord?

    /**
     * 경기 ID로 모든 투수 기록을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
    """)
    fun findAllByGameId(@Param("gameId") gameId: Long): List<PitchingRecord>

    /**
     * 선수 ID로 모든 투수 기록을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.player.id = :playerId
        ORDER BY gp.game.scheduledDate DESC
    """)
    fun findAllByPlayerId(@Param("playerId") playerId: Long): List<PitchingRecord>

    /**
     * 선수의 최근 N경기 투수 기록을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.player.id = :playerId
        ORDER BY gp.game.scheduledDate DESC
        LIMIT :limit
    """)
    fun findRecentByPlayerId(
        @Param("playerId") playerId: Long,
        @Param("limit") limit: Int
    ): List<PitchingRecord>

    /**
     * 팀의 특정 경기 투수 기록을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        JOIN gp.gameTeam gt
        WHERE gt.team.id = :teamId
        AND gp.game.id = :gameId
    """)
    fun findAllByTeamIdAndGameId(
        @Param("teamId") teamId: Long,
        @Param("gameId") gameId: Long
    ): List<PitchingRecord>

    /**
     * 경기의 선발 투수 기록을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.isStartingPitcher = true
    """)
    fun findStartingPitchersByGameId(@Param("gameId") gameId: Long): List<PitchingRecord>

    /**
     * 경기의 구원 투수 기록을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.isStartingPitcher = false
    """)
    fun findReliefPitchersByGameId(@Param("gameId") gameId: Long): List<PitchingRecord>

    /**
     * 특정 결정(WIN/LOSS/SAVE/HOLD)을 받은 투수 기록을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        WHERE pr.decision = :decision
    """)
    fun findAllByDecision(@Param("decision") decision: PitchingDecision): List<PitchingRecord>

    /**
     * 경기의 승리 투수를 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.decision = 'WIN'
    """)
    fun findWinningPitcherByGameId(@Param("gameId") gameId: Long): PitchingRecord?

    /**
     * 경기의 패전 투수를 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.decision = 'LOSS'
    """)
    fun findLosingPitcherByGameId(@Param("gameId") gameId: Long): PitchingRecord?

    /**
     * 경기의 세이브 투수를 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        JOIN pr.gamePlayer gp
        WHERE gp.game.id = :gameId
        AND pr.decision = 'SAVE'
    """)
    fun findSavePitcherByGameId(@Param("gameId") gameId: Long): PitchingRecord?

    /**
     * ERA 상위 N명을 조회합니다 (최소 이닝 조건).
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        WHERE pr.inningsPitchedOuts >= :minInningsPitchedOuts
        ORDER BY (CAST(pr.earnedRuns AS double) * 27.0 / CAST(pr.inningsPitchedOuts AS double)) ASC
        LIMIT :limit
    """)
    fun findTopByEarnedRunAverage(
        @Param("minInningsPitchedOuts") minInningsPitchedOuts: Int,
        @Param("limit") limit: Int
    ): List<PitchingRecord>

    /**
     * 삼진 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        ORDER BY pr.strikeouts DESC
        LIMIT :limit
    """)
    fun findTopByStrikeouts(@Param("limit") limit: Int): List<PitchingRecord>

    /**
     * 승수 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        WHERE pr.decision = 'WIN'
        ORDER BY pr.id DESC
        LIMIT :limit
    """)
    fun findTopByWins(@Param("limit") limit: Int): List<PitchingRecord>

    /**
     * 세이브 상위 N명을 조회합니다.
     */
    @Query("""
        SELECT pr FROM PitchingRecord pr
        WHERE pr.decision = 'SAVE'
        ORDER BY pr.id DESC
        LIMIT :limit
    """)
    fun findTopBySaves(@Param("limit") limit: Int): List<PitchingRecord>
}
