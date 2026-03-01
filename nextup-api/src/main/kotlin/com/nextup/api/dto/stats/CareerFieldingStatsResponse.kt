package com.nextup.api.dto.stats

import java.time.Instant

/**
 * 통산 수비 통계 응답 DTO
 */
data class CareerFieldingStatsResponse(
    val id: Long,
    val playerId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    // 시즌 및 출전 정보
    val seasonsPlayed: Int,
    val gamesPlayed: Int,
    // 기본 수비 기록
    val putOuts: Int,
    val assists: Int,
    val errors: Int,
    val doublePlays: Int,
    val passedBalls: Int,
    // 계산 속성
    val totalChances: Int,
    val fieldingPercentage: String?,
)
