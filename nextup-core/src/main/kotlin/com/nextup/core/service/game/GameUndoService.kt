package com.nextup.core.service.game

import com.nextup.core.domain.game.GameEvent

/**
 * 경기 이벤트 되돌리기 서비스 인터페이스
 *
 * 마지막 이벤트를 되돌리는 Undo 기능을 담당합니다.
 */
interface GameUndoService {
    fun undoLastEvent(gameId: Long): GameEvent
}
