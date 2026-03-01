package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.BaseRunningResult
import com.nextup.core.domain.game.GameEvent
import java.time.Instant

/**
 * 주루 플레이 기록 응답 DTO
 */
data class BaseRunningResponse(
    val eventId: Long,
    val runnerId: Long?,
    val fromBase: Base,
    val toBase: Base,
    val result: BaseRunningResult,
    val resultDisplay: String,
    val inning: Int,
    val isTopInning: Boolean,
    val outCountBefore: Int,
    val outCountAfter: Int,
    val gameState: GameStateResponse,
    val eventTimestamp: Instant,
    val message: String,
)

/**
 * GameEvent를 BaseRunningResponse로 변환합니다.
 */
fun GameEvent.toBaseRunningResponse(): BaseRunningResponse {
    val result = requireNotNull(this.baseRunningResult) { "baseRunningResult must not be null for BASE_RUNNING event" }
    val fromBase = requireNotNull(this.fromBase) { "fromBase must not be null for BASE_RUNNING event" }
    val toBase = requireNotNull(this.toBase) { "toBase must not be null for BASE_RUNNING event" }
    return BaseRunningResponse(
        eventId = this.id,
        runnerId = this.runnerPlayer?.id,
        fromBase = fromBase,
        toBase = toBase,
        result = result,
        resultDisplay = result.displayName,
        inning = this.inning,
        isTopInning = this.isTopInning,
        outCountBefore = this.outCountBefore,
        outCountAfter = this.outCountAfter,
        gameState = this.game.gameState.toResponse(),
        eventTimestamp = this.eventTimestamp,
        message = "${result.displayName} 기록이 완료되었습니다.",
    )
}
