package com.nextup.core.service.game

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.service.game.dto.PositionChangeRequest
import com.nextup.core.service.game.dto.PositionSwapRequest

/**
 * 수비 위치 변경 서비스 인터페이스
 *
 * 경기 중 선수의 수비 포지션 변경 및 두 선수 간 포지션 교환을 담당합니다.
 */
interface GamePositionChangeService {
    /**
     * 단일 선수의 포지션을 변경합니다.
     *
     * @param gameId 경기 ID
     * @param request 포지션 변경 요청
     * @param scorerId 기록원 ID (잠금 소유자 검증용)
     * @return 생성된 포지션 변경 GameEvent
     */
    fun changePosition(
        gameId: Long,
        request: PositionChangeRequest,
        scorerId: Long,
    ): GameEvent

    /**
     * 두 선수의 포지션을 교환합니다.
     *
     * @param gameId 경기 ID
     * @param request 포지션 교환 요청
     * @param scorerId 기록원 ID (잠금 소유자 검증용)
     * @return 생성된 포지션 변경 GameEvent 목록 (2개)
     */
    fun swapPositions(
        gameId: Long,
        request: PositionSwapRequest,
        scorerId: Long,
    ): List<GameEvent>
}
