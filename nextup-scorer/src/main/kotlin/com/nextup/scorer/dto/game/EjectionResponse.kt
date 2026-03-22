package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.GameEvent

/**
 * 퇴장 처리 응답 DTO
 */
data class EjectionResponse(
    val eventId: Long,
    val inning: Int,
    val isTopInning: Boolean,
    val ejectedPlayerId: Long?,
    val description: String,
)

/**
 * GameEvent(EJECTION)를 EjectionResponse로 변환합니다.
 */
fun GameEvent.toEjectionResponse(): EjectionResponse =
    EjectionResponse(
        eventId = this.id,
        inning = this.inning,
        isTopInning = this.isTopInning,
        ejectedPlayerId = this.batter?.id,
        description = this.description,
    )

/**
 * 긴급 교체 응답 DTO
 */
data class EmergencySubstitutionResponse(
    val eventId: Long,
    val inning: Int,
    val isTopInning: Boolean,
    val ejectedPlayerId: Long?,
    val replacementPlayerId: Long?,
    val description: String,
)

/**
 * GameEvent(EMERGENCY_SUBSTITUTION)를 EmergencySubstitutionResponse로 변환합니다.
 */
fun GameEvent.toEmergencySubstitutionResponse(): EmergencySubstitutionResponse =
    EmergencySubstitutionResponse(
        eventId = this.id,
        inning = this.inning,
        isTopInning = this.isTopInning,
        ejectedPlayerId = this.pitcher?.id,
        replacementPlayerId = this.batter?.id,
        description = this.description,
    )
