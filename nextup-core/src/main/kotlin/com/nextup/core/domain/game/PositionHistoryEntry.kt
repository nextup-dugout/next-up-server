package com.nextup.core.domain.game

import com.nextup.core.domain.player.Position

/**
 * 포지션 변경 이력 항목 (Value Object)
 *
 * 특정 이닝에서의 포지션 변경을 나타냅니다.
 *
 * @property inning 포지션이 변경된 이닝
 * @property position 해당 이닝에서의 포지션
 */
data class PositionHistoryEntry(
    val inning: Int,
    val position: Position,
) {
    init {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
    }
}
