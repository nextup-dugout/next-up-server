package com.nextup.api.dto.game

import java.time.Instant

/**
 * 경기 타임라인 응답 DTO
 */
data class GameTimelineResponse(
    val gameId: Long,
    val events: List<TimelineEventResponse>,
    val totalEvents: Int,
)

/**
 * 타임라인 이벤트 응답 DTO
 */
data class TimelineEventResponse(
    val eventId: Long,
    val inning: Int,
    val isTopInning: Boolean,
    val inningDisplay: String,
    val eventType: String,
    val description: String,
    val batterId: Long?,
    val batterName: String?,
    val pitcherId: Long?,
    val pitcherName: String?,
    val plateAppearanceResult: String?,
    val runsScored: Int,
    val outCountBefore: Int,
    val outCountAfter: Int,
    val eventTimestamp: Instant,
)
