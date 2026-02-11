package com.nextup.scorer.dto.websocket

import java.time.Instant

/**
 * 경기 이벤트 메시지 DTO
 *
 * 개별 경기 이벤트(타석 결과, 교체 등)를 실시간으로 브로드캐스트합니다.
 * Topic: /topic/games/{gameId}/events
 */
data class GameEventMessage(
    val eventId: Long,
    val eventType: String,
    val inning: Int,
    val isTopInning: Boolean,
    val description: String,
    val batter: PlayerBriefDto?,
    val pitcher: PlayerBriefDto?,
    val result: String?,
    val runsScored: Int,
    val timestamp: Instant
)
