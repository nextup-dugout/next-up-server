package com.nextup.core.domain.event

import java.time.Instant

/**
 * 경기 취소 도메인 이벤트
 *
 * 경기가 취소될 때 발행됩니다.
 * 해당 경기에 실시간으로 반영된 시즌 타격/투구 통계를 롤백하기 위해 사용됩니다.
 *
 * 정책:
 * - 취소 경기는 개인 기록이 무효화됩니다 (SeasonBattingStats/SeasonPitchingStats 롤백).
 * - PlateAppearanceRecordedEvent로 반영된 모든 실시간 스탯 기여분을 역산합니다.
 */
data class GameCancelledEvent(
    val gameId: Long,
    val timestamp: Instant = Instant.now(),
)
