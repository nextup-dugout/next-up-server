package com.nextup.core.port.repository

import com.nextup.core.domain.stats.CareerBattingStats
import java.util.Optional

/**
 * CareerBattingStats Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface CareerBattingStatsRepositoryPort {

    fun save(careerBattingStats: CareerBattingStats): CareerBattingStats

    fun findAll(): List<CareerBattingStats>

    fun delete(careerBattingStats: CareerBattingStats)

    fun deleteById(id: Long)

    /**
     * 선수 ID로 통산 타격 통계를 조회합니다.
     */
    fun findByPlayerId(playerId: Long): CareerBattingStats?

    /**
     * 통산 타율 상위 N명을 조회합니다 (최소 타수 조건).
     */
    fun findTopByBattingAverage(minAtBats: Int, limit: Int): List<CareerBattingStats>

    /**
     * 통산 홈런 상위 N명을 조회합니다.
     */
    fun findTopByHomeRuns(limit: Int): List<CareerBattingStats>

    /**
     * 통산 타점 상위 N명을 조회합니다.
     */
    fun findTopByRunsBattedIn(limit: Int): List<CareerBattingStats>

    /**
     * 통산 안타 상위 N명을 조회합니다.
     */
    fun findTopByHits(limit: Int): List<CareerBattingStats>

    /**
     * 통산 도루 상위 N명을 조회합니다.
     */
    fun findTopByStolenBases(limit: Int): List<CareerBattingStats>

    /**
     * 통산 OPS 상위 N명을 조회합니다 (최소 타석 조건).
     */
    fun findTopByOps(minPlateAppearances: Int, limit: Int): List<CareerBattingStats>

    /**
     * 통산 장타율 상위 N명을 조회합니다 (최소 타수 조건).
     */
    fun findTopBySluggingPercentage(minAtBats: Int, limit: Int): List<CareerBattingStats>
}
