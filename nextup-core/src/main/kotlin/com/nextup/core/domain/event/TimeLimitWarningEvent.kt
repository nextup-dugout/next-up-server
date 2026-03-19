package com.nextup.core.domain.event

import java.time.Instant

/**
 * 시간 제한 경고 이벤트
 *
 * 경기의 시간 제한(timeLimitMinutes)에 임박하거나 초과했을 때 발행됩니다.
 * 사회인 야구 시간 제한 규칙 지원을 위한 알림 이벤트입니다.
 *
 * @property gameId 경기 ID
 * @property startedAt 경기 시작 시각
 * @property timeLimitMinutes 설정된 시간 제한 (분)
 * @property elapsedMinutes 경과 시간 (분)
 * @property warningType 경고 유형
 */
data class TimeLimitWarningEvent(
    val gameId: Long,
    val startedAt: Instant,
    val timeLimitMinutes: Int,
    val elapsedMinutes: Long,
    val warningType: TimeLimitWarningType,
) {
    /** 남은 시간 (분, 초과 시 음수) */
    val remainingMinutes: Long
        get() = timeLimitMinutes - elapsedMinutes
}

/**
 * 시간 제한 경고 유형
 */
enum class TimeLimitWarningType(
    val displayName: String,
) {
    /** 시간 제한 임박 (10분 이하 남음) */
    APPROACHING_LIMIT("시간 제한 임박"),

    /** 시간 제한 도달 */
    LIMIT_REACHED("시간 제한 도달"),
}
