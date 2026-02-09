package com.nextup.core.service

import com.nextup.core.domain.game.PitchEvent
import com.nextup.core.domain.game.PitchResult

/**
 * 투구 이벤트 Service Interface
 *
 * 투구 이벤트 관련 비즈니스 로직을 정의합니다.
 */
interface PitchEventService {
    /**
     * 투구 이벤트를 기록합니다.
     */
    fun recordPitch(
        gameId: Long,
        pitcherId: Long,
        batterId: Long,
        result: PitchResult,
        description: String? = null,
    ): PitchEvent

    /**
     * 투구 이벤트를 조회합니다.
     */
    fun getPitchEvent(pitchEventId: Long): PitchEvent

    /**
     * 경기의 투구 이벤트 목록을 조회합니다.
     */
    fun getGamePitchEvents(gameId: Long): List<PitchEvent>

    /**
     * 경기의 특정 이닝 투구 이벤트 목록을 조회합니다.
     */
    fun getInningPitchEvents(
        gameId: Long,
        inning: Int,
        isTopInning: Boolean,
    ): List<PitchEvent>

    /**
     * 경기의 현재 볼카운트를 조회합니다.
     */
    fun getCurrentBallCount(gameId: Long): Pair<Int, Int>

    /**
     * 투수의 투구 통계를 계산합니다.
     */
    fun calculatePitcherStats(pitcherId: Long): PitcherPitchStats

    /**
     * 타자별 투구 통계를 계산합니다.
     */
    fun calculateBatterPitchStats(batterId: Long): BatterPitchStats
}

/**
 * 투수 투구 통계
 */
data class PitcherPitchStats(
    val totalPitches: Int,
    val strikes: Int,
    val balls: Int,
    val fouls: Int,
    val inPlayPitches: Int,
    val strikePercentage: Double,
    val avgPitchesPerAtBat: Double,
)

/**
 * 타자 투구 통계
 */
data class BatterPitchStats(
    val totalPitches: Int,
    val strikes: Int,
    val balls: Int,
    val fouls: Int,
    val swingAndMiss: Int,
    val avgPitchesPerAtBat: Double,
)
