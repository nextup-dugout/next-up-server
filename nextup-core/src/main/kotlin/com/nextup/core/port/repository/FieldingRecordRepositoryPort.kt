package com.nextup.core.port.repository

import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.player.Position

/**
 * FieldingRecord Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface FieldingRecordRepositoryPort {
    fun save(fieldingRecord: FieldingRecord): FieldingRecord

    fun findAll(): List<FieldingRecord>

    /**
     * ID로 수비 기록을 조회합니다.
     */
    fun findByIdOrNull(id: Long): FieldingRecord?

    fun delete(fieldingRecord: FieldingRecord)

    fun deleteById(id: Long)

    /**
     * GamePlayer로 수비 기록을 조회합니다.
     */
    fun findByGamePlayer(gamePlayer: GamePlayer): FieldingRecord?

    /**
     * GamePlayer ID로 수비 기록을 조회합니다.
     */
    fun findByGamePlayerId(gamePlayerId: Long): FieldingRecord?

    /**
     * 경기 ID로 모든 수비 기록을 조회합니다.
     */
    fun findAllByGameId(gameId: Long): List<FieldingRecord>

    /**
     * 선수 ID로 모든 수비 기록을 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<FieldingRecord>

    /**
     * GamePlayer와 포지션으로 수비 기록을 조회합니다.
     * 포지션별 수비 기록 분리를 위해 사용됩니다.
     */
    fun findByGamePlayerAndPosition(
        gamePlayer: GamePlayer,
        position: Position,
    ): FieldingRecord?

    /**
     * GamePlayer의 모든 수비 기록을 조회합니다.
     * 한 경기에서 여러 포지션을 소화한 선수의 포지션별 기록을 모두 조회합니다.
     */
    fun findAllByGamePlayer(gamePlayer: GamePlayer): List<FieldingRecord>
}
