package com.nextup.core.service.game.dto

import com.nextup.core.domain.game.Base

/**
 * 주자 이동 정보
 *
 * @property runnerId 주자 ID (GamePlayer ID)
 * @property fromBase 출발 베이스
 * @property toBase 도착 베이스
 * @property isOut 아웃 여부
 */
data class RunnerMovement(
    val runnerId: Long,
    val fromBase: Base,
    val toBase: Base,
    val isOut: Boolean = false
) {
    /**
     * 득점 여부를 확인합니다.
     */
    val isScored: Boolean
        get() = toBase == Base.HOME && !isOut
}
