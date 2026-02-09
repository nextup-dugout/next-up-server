package com.nextup.api.dto.attendance

import com.nextup.core.service.attendance.NudgeResult

/**
 * 출석 재촉 응답 DTO
 */
data class NudgeResponse(
    val notifiedCount: Int,
    val nonVoterNames: List<String>,
) {
    companion object {
        fun from(result: NudgeResult): NudgeResponse =
            NudgeResponse(
                notifiedCount = result.notifiedCount,
                nonVoterNames = result.nonVoterNames,
            )
    }
}
