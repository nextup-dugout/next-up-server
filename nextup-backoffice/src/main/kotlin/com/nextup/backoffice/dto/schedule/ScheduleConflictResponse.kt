package com.nextup.backoffice.dto.schedule

import com.nextup.core.domain.schedule.ConflictType
import com.nextup.core.domain.schedule.ScheduleConflict

/**
 * 대진표 충돌 응답 DTO
 */
data class ScheduleConflictResponse(
    val type: ConflictType,
    val conflictingScheduleId: Long,
    val description: String,
) {
    companion object {
        fun from(conflict: ScheduleConflict): ScheduleConflictResponse =
            ScheduleConflictResponse(
                type = conflict.type,
                conflictingScheduleId = conflict.conflictingScheduleId,
                description = conflict.description,
            )
    }
}
