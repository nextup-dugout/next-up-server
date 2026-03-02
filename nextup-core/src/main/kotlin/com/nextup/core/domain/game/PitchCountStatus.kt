package com.nextup.core.domain.game

/**
 * 투구 수 제한 상태
 *
 * 투수의 현재 투구 수가 규칙상 제한과 비교하여 어떤 상태인지를 나타냅니다.
 */
enum class PitchCountStatus(
    val displayName: String,
) {
    /** 투구 수 제한 임박 (threshold 이하 남음) */
    APPROACHING_LIMIT("투구 수 제한 임박"),

    /** 투구 수 제한 도달 */
    LIMIT_REACHED("투구 수 제한 도달"),
}
