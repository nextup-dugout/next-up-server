package com.nextup.core.service.game.dto

import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.BaseRunningResult

/**
 * 주루 플레이 기록 요청 DTO (core 서비스 계층)
 *
 * @property runnerId 주자 ID (GamePlayer ID)
 * @property fromBase 출발 베이스
 * @property toBase 도착 베이스
 * @property result 주루 플레이 결과
 */
data class BaseRunningRequest(
    val runnerId: Long,
    val fromBase: Base,
    val toBase: Base,
    val result: BaseRunningResult,
)
