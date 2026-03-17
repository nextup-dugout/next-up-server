package com.nextup.api.dto.game

import java.time.Instant

/**
 * 수비 기록 응답 DTO (API 모듈)
 *
 * 경기별 수비 기록 조회 시 사용됩니다.
 */
data class FieldingRecordResponse(
    val id: Long,
    val gamePlayerId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val putOuts: Int,
    val assists: Int,
    val errors: Int,
    val doublePlays: Int,
    val passedBalls: Int,
    val totalChances: Int,
    val fieldingPercentage: String?,
)
