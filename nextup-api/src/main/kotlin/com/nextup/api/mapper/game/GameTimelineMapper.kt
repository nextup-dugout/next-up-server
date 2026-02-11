package com.nextup.api.mapper.game

import com.nextup.api.dto.game.GameTimelineResponse
import com.nextup.api.dto.game.TimelineEventResponse
import com.nextup.core.service.game.dto.GameTimelineDto
import com.nextup.core.service.game.dto.TimelineEventDto

/**
 * GameTimelineDto를 GameTimelineResponse로 변환합니다.
 */
fun GameTimelineDto.toResponse(): GameTimelineResponse =
    GameTimelineResponse(
        gameId = gameId,
        events = events.map { it.toResponse() },
        totalEvents = totalEvents,
    )

/**
 * TimelineEventDto를 TimelineEventResponse로 변환합니다.
 */
fun TimelineEventDto.toResponse(): TimelineEventResponse =
    TimelineEventResponse(
        eventId = eventId,
        inning = inning,
        isTopInning = isTopInning,
        inningDisplay = inningDisplay,
        eventType = eventType,
        description = description,
        batterId = batterId,
        batterName = batterName,
        pitcherId = pitcherId,
        pitcherName = pitcherName,
        plateAppearanceResult = plateAppearanceResult,
        runsScored = runsScored,
        outCountBefore = outCountBefore,
        outCountAfter = outCountAfter,
        eventTimestamp = eventTimestamp,
    )
