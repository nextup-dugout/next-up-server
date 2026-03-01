package com.nextup.core.port.repository

import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer

/**
 * FieldingRecord Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface FieldingRecordRepositoryPort {
    fun save(fieldingRecord: FieldingRecord): FieldingRecord

    fun findAll(): List<FieldingRecord>

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
}
