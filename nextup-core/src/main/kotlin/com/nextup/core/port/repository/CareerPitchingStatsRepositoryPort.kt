package com.nextup.core.port.repository

import com.nextup.core.domain.stats.CareerPitchingStats
import java.util.Optional

/**
 * CareerPitchingStats Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface CareerPitchingStatsRepositoryPort {

    fun save(careerPitchingStats: CareerPitchingStats): CareerPitchingStats

    fun findAll(): List<CareerPitchingStats>

    fun delete(careerPitchingStats: CareerPitchingStats)

    fun deleteById(id: Long)

    /**
     * 선수 ID로 통산 투수 통계를 조회합니다.
     */
    fun findByPlayerId(playerId: Long): CareerPitchingStats?

    /**
     * 통산 ERA 상위 N명을 조회합니다 (최소 이닝 조건).
     * ERA는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    fun findTopByEra(minInningsPitchedOuts: Int, limit: Int): List<CareerPitchingStats>

    /**
     * 통산 승수 상위 N명을 조회합니다.
     */
    fun findTopByWins(limit: Int): List<CareerPitchingStats>

    /**
     * 통산 삼진 상위 N명을 조회합니다.
     */
    fun findTopByStrikeouts(limit: Int): List<CareerPitchingStats>

    /**
     * 통산 세이브 상위 N명을 조회합니다.
     */
    fun findTopBySaves(limit: Int): List<CareerPitchingStats>

    /**
     * 통산 WHIP 상위 N명을 조회합니다 (최소 이닝 조건).
     * WHIP는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    fun findTopByWhip(minInningsPitchedOuts: Int, limit: Int): List<CareerPitchingStats>

    /**
     * 통산 이닝 상위 N명을 조회합니다.
     */
    fun findTopByInningsPitched(limit: Int): List<CareerPitchingStats>

    /**
     * 통산 삼진/볼넷 비율 상위 N명을 조회합니다 (최소 이닝 및 볼넷 조건).
     */
    fun findTopByStrikeoutToWalkRatio(minInningsPitchedOuts: Int, limit: Int): List<CareerPitchingStats>
}
