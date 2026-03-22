package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GamePlayerRepository :
    JpaRepository<GamePlayer, Long>,
    GamePlayerRepositoryPort {
    override fun findByIdOrNull(id: Long): GamePlayer? = findById(id).orElse(null)

    /**
     * 경기 ID와 선수 ID로 GamePlayer를 조회합니다.
     */
    @Query(
        """
        SELECT gp FROM GamePlayer gp
        JOIN FETCH gp.gameTeam gt
        JOIN FETCH gt.game
        JOIN FETCH gt.team
        JOIN FETCH gp.player
        WHERE gt.game.id = :gameId
        AND gp.player.id = :playerId
    """,
    )
    override fun findByGameIdAndPlayerId(
        @Param("gameId") gameId: Long,
        @Param("playerId") playerId: Long,
    ): GamePlayer?

    /**
     * 경기 ID로 모든 GamePlayer를 조회합니다.
     */
    @Query(
        """
        SELECT gp FROM GamePlayer gp
        JOIN FETCH gp.gameTeam gt
        JOIN FETCH gt.game
        JOIN FETCH gt.team
        JOIN FETCH gp.player
        WHERE gt.game.id = :gameId
    """,
    )
    override fun findAllByGameId(
        @Param("gameId") gameId: Long,
    ): List<GamePlayer>

    /**
     * 선수 ID로 모든 GamePlayer를 조회합니다.
     */
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.player.id = :playerId")
    override fun findAllByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<GamePlayer>

    /**
     * 경기에서 현재 출전 중인 GamePlayer를 조회합니다.
     */
    @Query(
        """
        SELECT gp FROM GamePlayer gp
        JOIN gp.gameTeam gt
        WHERE gt.game.id = :gameId
        AND gp.isCurrentlyPlaying = true
    """,
    )
    override fun findCurrentlyPlayingByGameId(
        @Param("gameId") gameId: Long,
    ): List<GamePlayer>
}
