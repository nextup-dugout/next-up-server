package com.nextup.core.service.game.dto

import java.time.Instant

/**
 * 경기 타임라인 DTO
 *
 * 경기의 모든 이벤트를 시간 순서대로 제공합니다.
 */
data class GameTimelineDto(
    val gameId: Long,
    val events: List<TimelineEventDto>,
    val totalEvents: Int,
)

/**
 * 타임라인 이벤트 DTO
 *
 * 경기 중 발생한 개별 이벤트 정보를 담습니다.
 */
data class TimelineEventDto(
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
