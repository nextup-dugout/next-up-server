package com.nextup.core.service.game.dto

/**
 * 출석 투표 요약 DTO
 */
data class AttendanceSummaryDto(
    val gameId: Long,
    val totalMembers: Int,
    val attending: Int,
    val absent: Int,
    val undecided: Int,
) {
    /**
     * 응답률을 계산합니다.
     */
    val responseRate: Double
        get() =
            if (totalMembers == 0) {
                0.0
            } else {
                (attending + absent).toDouble() / totalMembers
            }
}
