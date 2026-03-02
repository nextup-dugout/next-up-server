package com.nextup.core.service.game.dto

import com.nextup.core.domain.player.Position

/**
 * 선수 교체 요청 도메인 DTO
 *
 * @param gameTeamId 교체가 발생하는 팀의 GameTeam ID
 * @param outgoingPlayerId 교체 나가는 선수의 GamePlayer ID
 * @param incomingPlayerId 교체 들어오는 선수의 GamePlayer ID
 * @param newPosition 교체 들어오는 선수의 새 포지션
 * @param newBattingOrder 교체 들어오는 선수의 타순 (null이면 타순 없음)
 */
data class SubstitutionRequest(
    val gameTeamId: Long,
    val outgoingPlayerId: Long,
    val incomingPlayerId: Long,
    val newPosition: Position,
    val newBattingOrder: Int?,
)
