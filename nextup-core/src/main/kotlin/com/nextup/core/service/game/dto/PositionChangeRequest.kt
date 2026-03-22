package com.nextup.core.service.game.dto

import com.nextup.core.domain.player.Position

/**
 * 포지션 변경 요청 도메인 DTO
 *
 * @param playerId 포지션을 변경할 선수의 GamePlayer ID
 * @param newPosition 새로운 포지션
 */
data class PositionChangeRequest(
    val playerId: Long,
    val newPosition: Position,
)

/**
 * 포지션 교환 요청 도메인 DTO
 *
 * @param player1Id 포지션을 교환할 첫 번째 선수의 GamePlayer ID
 * @param player2Id 포지션을 교환할 두 번째 선수의 GamePlayer ID
 */
data class PositionSwapRequest(
    val player1Id: Long,
    val player2Id: Long,
)
