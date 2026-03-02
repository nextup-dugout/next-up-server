package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.GameEvent

/**
 * 선수 교체 응답 DTO
 */
data class SubstitutionResponse(
    val eventId: Long,
    val inning: Int,
    val isTopInning: Boolean,
    val incomingPlayerId: Long?,
    val outgoingPlayerId: Long?,
    val description: String,
)

/**
 * GameEvent(SUBSTITUTION)를 SubstitutionResponse로 변환합니다.
 */
fun GameEvent.toSubstitutionResponse(): SubstitutionResponse =
    SubstitutionResponse(
        eventId = this.id,
        inning = this.inning,
        isTopInning = this.isTopInning,
        incomingPlayerId = this.batter?.id,
        outgoingPlayerId = this.pitcher?.id,
        description = this.description,
    )
