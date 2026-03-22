package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.GameEvent

/**
 * 포지션 변경 응답 DTO
 */
data class PositionChangeResponse(
    val eventId: Long,
    val inning: Int,
    val isTopInning: Boolean,
    val playerId: Long?,
    val description: String,
)

/**
 * GameEvent(POSITION_CHANGE)를 PositionChangeResponse로 변환합니다.
 */
fun GameEvent.toPositionChangeResponse(): PositionChangeResponse =
    PositionChangeResponse(
        eventId = this.id,
        inning = this.inning,
        isTopInning = this.isTopInning,
        playerId = this.batter?.id,
        description = this.description,
    )

/**
 * 포지션 교환 응답 DTO (2개의 이벤트를 담습니다)
 */
data class PositionSwapResponse(
    val events: List<PositionChangeResponse>,
)

/**
 * GameEvent 목록을 PositionSwapResponse로 변환합니다.
 */
fun List<GameEvent>.toPositionSwapResponse(): PositionSwapResponse =
    PositionSwapResponse(
        events = this.map { it.toPositionChangeResponse() },
    )
