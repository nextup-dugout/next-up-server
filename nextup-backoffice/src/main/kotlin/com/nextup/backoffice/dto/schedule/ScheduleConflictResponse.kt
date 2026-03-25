package com.nextup.backoffice.dto.schedule

import com.nextup.core.domain.schedule.ConflictType

/**
 * 대진표 충돌 응답 DTO
 *
 * 변환 로직은 ScheduleExtensions.kt의 Extension Function을 사용합니다.
 */
data class ScheduleConflictResponse(
    val type: ConflictType,
    val conflictingScheduleId: Long,
    val description: String,
)
