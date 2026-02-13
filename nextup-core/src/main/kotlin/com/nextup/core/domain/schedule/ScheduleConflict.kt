package com.nextup.core.domain.schedule

/**
 * 대진표 충돌 정보
 *
 * 대진표 생성/수정 시 발생할 수 있는 충돌 정보를 담습니다.
 */
data class ScheduleConflict(
    val type: ConflictType,
    val conflictingScheduleId: Long,
    val description: String,
)

/**
 * 충돌 유형
 */
enum class ConflictType(
    val displayName: String,
) {
    TEAM_TIME_CONFLICT("팀 시간 충돌"),
    VENUE_TIME_CONFLICT("경기장 시간 충돌"),
}
