package com.nextup.api.dto.attendance

import com.nextup.core.domain.game.AttendanceStatus
import java.time.LocalDateTime

/**
 * 출석 투표 응답 DTO
 */
data class AttendanceVoteResponse(
    val voteId: Long,
    val gameId: Long,
    val memberId: Long,
    val status: AttendanceStatus,
    val reason: String?,
    val respondedAt: LocalDateTime?,
)

/**
 * 출석 투표 요약 응답 DTO
 */
data class AttendanceSummaryResponse(
    val gameId: Long,
    val totalMembers: Int,
    val attending: Int,
    val absent: Int,
    val undecided: Int,
    val responseRate: Double,
)

/**
 * 멤버별 투표 현황 응답 DTO
 */
data class MemberVoteResponse(
    val voteId: Long,
    val member: MemberSummary,
    val status: AttendanceStatus,
    val reason: String?,
    val respondedAt: LocalDateTime?,
)

/**
 * 멤버 요약 정보
 */
data class MemberSummary(
    val memberId: Long,
    val nickname: String,
    val uniformNumber: Int,
    val position: String,
)

/**
 * 투표 현황 전체 응답 DTO
 */
data class AttendanceVotesResponse(
    val gameId: Long,
    val gameDate: LocalDateTime,
    val votes: List<MemberVoteResponse>,
    val summary: AttendanceSummaryResponse,
)
