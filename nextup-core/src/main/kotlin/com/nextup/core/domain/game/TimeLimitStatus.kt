package com.nextup.core.domain.game

/**
 * 시간 제한 상태
 *
 * 경기의 현재 경과 시간이 규칙상 시간 제한과 비교하여 어떤 상태인지를 나타냅니다.
 */
enum class TimeLimitStatus(
    val displayName: String,
) {
    /** 시간 제한 임박 (threshold 이하 남음) */
    APPROACHING_LIMIT("시간 제한 임박"),

    /** 시간 제한 도달 */
    LIMIT_REACHED("시간 제한 도달"),
}
