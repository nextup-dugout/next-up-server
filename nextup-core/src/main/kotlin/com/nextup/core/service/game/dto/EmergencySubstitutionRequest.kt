package com.nextup.core.service.game.dto

import com.nextup.core.domain.game.EjectionReason
import com.nextup.core.domain.player.Position

/**
 * 퇴장 처리 요청 도메인 DTO
 *
 * 교체 없이 퇴장만 처리하는 경우에 사용합니다.
 *
 * @param ejectedPlayerId 퇴장할 선수의 GamePlayer ID
 * @param reason 퇴장 사유
 */
data class EjectionRequest(
    val ejectedPlayerId: Long,
    val reason: EjectionReason,
)

/**
 * 퇴장 + 긴급 교체 요청 도메인 DTO
 *
 * 퇴장과 교체를 원자적으로 처리하는 경우에 사용합니다.
 * 주자 상태인 선수 퇴장 시 교체 선수가 해당 베이스를 계승합니다.
 *
 * @param ejectedPlayerId 퇴장할 선수의 GamePlayer ID
 * @param replacementPlayerId 교체 투입할 선수의 GamePlayer ID
 * @param reason 퇴장 사유
 * @param newPosition 교체 선수의 포지션
 */
data class EjectAndSubstituteRequest(
    val ejectedPlayerId: Long,
    val replacementPlayerId: Long,
    val reason: EjectionReason,
    val newPosition: Position,
)
