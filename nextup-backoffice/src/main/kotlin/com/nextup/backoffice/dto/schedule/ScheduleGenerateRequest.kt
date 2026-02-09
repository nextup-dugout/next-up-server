package com.nextup.backoffice.dto.schedule

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

/**
 * 대진표 자동 생성 요청 DTO (관리자용)
 */
data class ScheduleGenerateRequest(
    @field:NotEmpty(message = "참가 팀 목록은 비어있을 수 없습니다")
    val teamIds: List<
        @Positive(message = "팀 ID는 양수여야 합니다")
        Long,
    >,
    val doubleRoundRobin: Boolean = false,
)
