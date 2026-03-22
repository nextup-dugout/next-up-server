package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BattingRecordRepository :
    JpaRepository<BattingRecord, Long>,
    BattingRecordRepositoryPort {
    /**
     * ID로 타격 기록을 조회합니다.
     */
    override fun findByIdOrNull(id: Long): BattingRecord? = findById(id).orElse(null)

    /**
     * GamePlayer로 타격 기록을 조회합니다.
     */
    override fun findByGamePlayer(gamePlayer: GamePlayer): BattingRecord?

    /**
     * GamePlayer ID로 타격 기록을 조회합니다.
     */
    @Query("SELECT br FROM BattingRecord br WHERE br.gamePlayer.id = :gamePlayerId")
    override fun findByGamePlayerId(
        @Param("gamePlayerId") gamePlayerId: Long,
    ): BattingRecord?

    /**
     * 경기 ID로 모든 타격 기록을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN br.gamePlayer gp
        WHERE gp.gameTeam.game.id = :gameId
    """,
    )
    override fun findAllByGameId(
        @Param("gameId") gameId: Long,
    ): List<BattingRecord>

    /**
     * 선수 ID로 모든 타격 기록을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN br.gamePlayer gp
        WHERE gp.player.id = :playerId
        ORDER BY gp.gameTeam.game.scheduledAt DESC
    """,
    )
    override fun findAllByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<BattingRecord>

    /**
     * 선수의 최근 N경기 타격 기록을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN br.gamePlayer gp
        WHERE gp.player.id = :playerId
        ORDER BY gp.gameTeam.game.scheduledAt DESC
        LIMIT :limit
    """,
    )
    override fun findRecentByPlayerId(
        @Param("playerId") playerId: Long,
        @Param("limit") limit: Int,
    ): List<BattingRecord>

    /**
     * 팀의 특정 경기 타격 기록을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN br.gamePlayer gp
        JOIN gp.gameTeam gt
        WHERE gt.team.id = :teamId
        AND gp.gameTeam.game.id = :gameId
    """,
    )
    override fun findAllByTeamIdAndGameId(
        @Param("teamId") teamId: Long,
        @Param("gameId") gameId: Long,
    ): List<BattingRecord>

    /**
     * 경기에서 최소 타석 수 이상인 타격 기록을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN br.gamePlayer gp
        WHERE gp.gameTeam.game.id = :gameId
        AND br.plateAppearances >= :minPlateAppearances
    """,
    )
    override fun findByGameIdAndMinPlateAppearances(
        @Param("gameId") gameId: Long,
        @Param("minPlateAppearances") minPlateAppearances: Int,
    ): List<BattingRecord>

    /**
     * 타율 상위 N명을 조회합니다 (최소 타수 조건).
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        WHERE br.atBats >= :minAtBats
        ORDER BY (CAST(br.hits AS double) / CAST(br.atBats AS double)) DESC
        LIMIT :limit
    """,
    )
    override fun findTopByBattingAverage(
        @Param("minAtBats") minAtBats: Int,
        @Param("limit") limit: Int,
    ): List<BattingRecord>

    /**
     * 홈런 상위 N명을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        ORDER BY br.homeRuns DESC
        LIMIT :limit
    """,
    )
    override fun findTopByHomeRuns(
        @Param("limit") limit: Int,
    ): List<BattingRecord>

    /**
     * 타점 상위 N명을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        ORDER BY br.runsBattedIn DESC
        LIMIT :limit
    """,
    )
    override fun findTopByRunsBattedIn(
        @Param("limit") limit: Int,
    ): List<BattingRecord>

    /**
     * 선수의 특정 연도 타격 기록을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN br.gamePlayer gp
        WHERE gp.player.id = :playerId
        AND FUNCTION('YEAR', gp.gameTeam.game.scheduledAt) = :year
        ORDER BY gp.gameTeam.game.scheduledAt DESC
    """,
    )
    override fun findAllByPlayerIdAndYear(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
    ): List<BattingRecord>

    /**
     * 팀의 여러 경기 타격 기록을 한 번에 조회합니다. (N+1 방지용 배치 쿼리)
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN br.gamePlayer gp
        JOIN gp.gameTeam gt
        WHERE gt.team.id = :teamId
        AND gp.gameTeam.game.id IN :gameIds
    """,
    )
    override fun findAllByTeamIdAndGameIds(
        @Param("teamId") teamId: Long,
        @Param("gameIds") gameIds: List<Long>,
    ): List<BattingRecord>

    /**
     * 선수 ID로 모든 타격 기록을 조회합니다. (FETCH JOIN으로 N+1 방지)
     * gamePlayer, gameTeam, game을 함께 로딩하여 커리어 통계 계산 시 추가 쿼리가 발생하지 않습니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN FETCH br.gamePlayer gp
        JOIN FETCH gp.gameTeam gt
        JOIN FETCH gt.game g
        WHERE gp.player.id = :playerId
        ORDER BY g.scheduledAt DESC
    """,
    )
    override fun findAllByPlayerIdWithGameInfo(
        @Param("playerId") playerId: Long,
    ): List<BattingRecord>

    /**
     * 선수 ID와 대회 ID로 타격 기록을 조회합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN FETCH br.gamePlayer gp
        JOIN FETCH gp.gameTeam gt
        JOIN FETCH gt.game g
        WHERE gp.player.id = :playerId
        AND g.competition.id = :competitionId
        ORDER BY g.scheduledAt DESC
    """,
    )
    override fun findAllByPlayerIdAndCompetitionId(
        @Param("playerId") playerId: Long,
        @Param("competitionId") competitionId: Long,
    ): List<BattingRecord>

    /**
     * 여러 경기 ID로 타격 기록을 한 번에 조회합니다. (N+1 방지용 배치 쿼리)
     * 빈 리스트 전달 시 빈 결과를 반환합니다.
     */
    @Query(
        """
        SELECT br FROM BattingRecord br
        JOIN FETCH br.gamePlayer gp
        JOIN FETCH gp.gameTeam gt
        WHERE gt.game.id IN :gameIds
    """,
    )
    override fun findAllByGameIds(
        @Param("gameIds") gameIds: List<Long>,
    ): List<BattingRecord>
}
