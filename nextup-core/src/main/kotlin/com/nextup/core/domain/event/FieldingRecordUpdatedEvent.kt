package com.nextup.core.domain.event

import java.time.Instant

/**
 * 수비 기록 갱신 도메인 이벤트
 *
 * 경기 중 수비 기록(자살, 보살, 실책 등)이 발생할 때 발행됩니다.
 * 시즌 수비 통계를 실시간으로 갱신하기 위해 사용됩니다.
 *
 * @param gameId 경기 ID
 * @param playerId 수비 선수 ID
 * @param type 수비 기록 유형
 * @param isRevert true이면 기록 취소(Undo)
 * @param timestamp 이벤트 발생 시각
 */
data class FieldingRecordUpdatedEvent(
    val gameId: Long,
    val playerId: Long,
    val type: FieldingEventType,
    val isRevert: Boolean = false,
    val timestamp: Instant = Instant.now(),
)

/**
 * 수비 기록 이벤트 유형
 */
enum class FieldingEventType {
    PUT_OUT,
    ASSIST,
    ERROR,
    DOUBLE_PLAY,
    PASSED_BALL,
    CAUGHT_STEALING,
    STOLEN_BASE_ALLOWED,
}
