package com.nextup.core.domain.event

import com.nextup.core.domain.game.PlateAppearanceResult
import java.time.Instant

/**
 * 타석 결과 취소 도메인 이벤트
 *
 * 경기 중 타석 결과가 Undo될 때 발행됩니다.
 * 시즌 타격 통계를 실시간으로 역산하기 위해 사용됩니다.
 */
data class PlateAppearanceUndoneEvent(
    val gameId: Long,
    val playerId: Long,
    val result: PlateAppearanceResult,
    val timestamp: Instant = Instant.now(),
)
