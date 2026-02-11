package com.nextup.core.port.repository

import com.nextup.core.domain.game.GameEvent

/**
 * GameEvent Repository Port
 */
interface GameEventRepositoryPort {
    fun save(gameEvent: GameEvent): GameEvent

    fun findByIdOrNull(id: Long): GameEvent?

    fun findAllByGameId(gameId: Long): List<GameEvent>

    fun findAllByGameIdOrderByEventTimestamp(gameId: Long): List<GameEvent>

    fun delete(gameEvent: GameEvent)

    /**
     * 경기의 마지막 활성 이벤트를 조회합니다 (undone=false 중 가장 최근).
     */
    fun findLastActiveEvent(gameId: Long): GameEvent?

    /**
     * 특정 투수-타자 매치업의 타석 결과를 조회합니다.
     */
    fun findPlateAppearancesByPitcherAndBatter(
        pitcherId: Long,
        batterId: Long,
    ): List<GameEvent>

    /**
     * 특정 연도의 투수-타자 매치업 타석 결과를 조회합니다.
     */
    fun findPlateAppearancesByPitcherAndBatterAndYear(
        pitcherId: Long,
        batterId: Long,
        year: Int,
    ): List<GameEvent>
}
