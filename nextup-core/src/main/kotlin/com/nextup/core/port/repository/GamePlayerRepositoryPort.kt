package com.nextup.core.port.repository

import com.nextup.core.domain.game.GamePlayer

/**
 * GamePlayer Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface GamePlayerRepositoryPort {
    fun save(gamePlayer: GamePlayer): GamePlayer

    fun findAll(): List<GamePlayer>

    fun findByIdOrNull(id: Long): GamePlayer?

    fun delete(gamePlayer: GamePlayer)

    fun deleteById(id: Long)

    /**
     * 경기 ID와 선수 ID로 GamePlayer를 조회합니다.
     */
    fun findByGameIdAndPlayerId(
        gameId: Long,
        playerId: Long,
    ): GamePlayer?

    /**
     * 경기 ID로 모든 GamePlayer를 조회합니다.
     */
    fun findAllByGameId(gameId: Long): List<GamePlayer>

    /**
     * 선수 ID로 모든 GamePlayer를 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<GamePlayer>

    /**
     * 경기에서 현재 출전 중인 GamePlayer를 조회합니다.
     */
    fun findCurrentlyPlayingByGameId(gameId: Long): List<GamePlayer>
}
