package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FieldingRecordRepository :
    JpaRepository<FieldingRecord, Long>,
    FieldingRecordRepositoryPort {
    /**
     * ID로 수비 기록을 조회합니다.
     */
    override fun findByIdOrNull(id: Long): FieldingRecord? = findById(id).orElse(null)

    /**
     * GamePlayer로 수비 기록을 조회합니다.
     */
    override fun findByGamePlayer(gamePlayer: GamePlayer): FieldingRecord?

    /**
     * GamePlayer ID로 수비 기록을 조회합니다.
     */
    @Query("SELECT fr FROM FieldingRecord fr WHERE fr.gamePlayer.id = :gamePlayerId")
    override fun findByGamePlayerId(
        @Param("gamePlayerId") gamePlayerId: Long,
    ): FieldingRecord?

    /**
     * 경기 ID로 모든 수비 기록을 조회합니다.
     */
    @Query(
        """
        SELECT fr FROM FieldingRecord fr
        JOIN fr.gamePlayer gp
        WHERE gp.gameTeam.game.id = :gameId
    """,
    )
    override fun findAllByGameId(
        @Param("gameId") gameId: Long,
    ): List<FieldingRecord>

    /**
     * 선수 ID로 모든 수비 기록을 조회합니다.
     */
    @Query(
        """
        SELECT fr FROM FieldingRecord fr
        JOIN fr.gamePlayer gp
        WHERE gp.player.id = :playerId
        ORDER BY gp.gameTeam.game.scheduledAt DESC
    """,
    )
    override fun findAllByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<FieldingRecord>

    /**
     * 선수의 특정 연도 수비 기록을 조회합니다.
     * L-7: 경기 종료 시 시즌 통계 교차 검증에 사용됩니다.
     */
    @Query(
        """
        SELECT fr FROM FieldingRecord fr
        JOIN fr.gamePlayer gp
        JOIN gp.gameTeam gt
        JOIN gt.game g
        WHERE gp.player.id = :playerId
        AND YEAR(g.scheduledAt) = :year
        ORDER BY g.scheduledAt DESC
    """,
    )
    override fun findAllByPlayerIdAndYear(
        @Param("playerId") playerId: Long,
        @Param("year") year: Int,
    ): List<FieldingRecord>
}
