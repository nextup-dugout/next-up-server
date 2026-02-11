package com.nextup.core.domain.game

/**
 * 출석 투표 상태
 */
enum class AttendanceStatus(
    val displayName: String,
) {
    /** 참석 */
    ATTENDING("참석"),

    /** 불참 */
    ABSENT("불참"),

    /** 미정 (기본값) */
    UNDECIDED("미정"),
}
