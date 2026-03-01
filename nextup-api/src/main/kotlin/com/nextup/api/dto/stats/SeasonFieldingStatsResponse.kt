package com.nextup.api.dto.stats

import java.time.Instant

/**
 * 시즌 수비 통계 응답 DTO
 */
data class SeasonFieldingStatsResponse(
    val id: Long,
    val playerId: Long,
    val year: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    // 출전 정보
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
