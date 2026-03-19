package com.nextup.core.service.game

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.service.game.dto.BaseRunningRequest

/**
 * 주루 플레이 기록 서비스 인터페이스
 *
 * 도루, 도루 실패, 견제사, 폭투 진루 등 타석 외 주루 이벤트를 기록합니다.
 */
interface BaseRunningRecordService {
    fun recordBaseRunning(
        gameId: Long,
        request: BaseRunningRequest,
        scorerId: Long,
    ): GameEvent
}
