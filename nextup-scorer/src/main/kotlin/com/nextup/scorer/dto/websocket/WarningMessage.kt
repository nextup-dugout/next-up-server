package com.nextup.scorer.dto.websocket

import java.time.Instant

/**
 * 경기 경고 메시지 DTO
 *
 * 경기 시간 제한, 투구수 제한 등 경고 알림을 기록원에게 실시간으로 브로드캐스트합니다.
 * Topic: /topic/games/{gameId}/warnings
 */
data class WarningMessage(
    val gameId: Long,
    val warningType: WarningType,
    val severity: WarningSeverity,
    val title: String,
    val message: String,
    val details: Map<String, Any?>,
    val timestamp: Instant,
)

/**
 * 경고 유형
 */
enum class WarningType {
    /** 시간 제한 경고 */
    TIME_LIMIT,

    /** 투구수 제한 경고 */
    PITCH_COUNT,
}

/**
 * 경고 심각도
 */
enum class WarningSeverity {
    /** 주의 — 제한에 임박 */
    WARNING,

    /** 위험 — 제한 도달 */
    CRITICAL,
}
