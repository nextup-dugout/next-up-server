package com.nextup.core.port.repository

import com.nextup.core.domain.game.PitchEvent

/**
 * 투구 이벤트 Repository Port
 *
 * 투구 이벤트 데이터 접근을 위한 인터페이스입니다.
 */
interface PitchEventRepositoryPort {
    /**
     * 투구 이벤트를 저장합니다.
     */
    fun save(pitchEvent: PitchEvent): PitchEvent

    /**
     * ID로 투구 이벤트를 조회합니다.
     */
    fun findByIdOrNull(id: Long): PitchEvent?

    /**
     * 경기 ID로 투구 이벤트 목록을 조회합니다.
     */
    fun findByGameId(gameId: Long): List<PitchEvent>

    /**
     * 경기 ID와 이닝으로 투구 이벤트 목록을 조회합니다.
     */
    fun findByGameIdAndInning(
        gameId: Long,
        inning: Int,
        isTopInning: Boolean,
    ): List<PitchEvent>

    /**
     * 투수 ID로 투구 이벤트 목록을 조회합니다.
     */
    fun findByPitcherId(pitcherId: Long): List<PitchEvent>

    /**
     * 타자 ID로 투구 이벤트 목록을 조회합니다.
     */
    fun findByBatterId(batterId: Long): List<PitchEvent>

    /**
     * 경기의 다음 투구 번호를 조회합니다.
     */
    fun getNextPitchNumber(gameId: Long): Int
}
