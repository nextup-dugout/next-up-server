package com.nextup.api.dto.attendance

import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.attendance.VoteType
import jakarta.validation.constraints.NotNull

/**
 * 출석 투표 요청 DTO
 */
data class AttendanceVoteRequest(
    @field:NotNull(message = "투표 유형은 필수입니다")
    val voteType: VoteType,
    val absenceReason: AbsenceReason? = null,
    val reasonDetail: String? = null,
)
