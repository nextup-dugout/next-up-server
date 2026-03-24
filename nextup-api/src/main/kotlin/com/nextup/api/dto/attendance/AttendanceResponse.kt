package com.nextup.api.dto.attendance

import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.attendance.VoteType

/**
 * 출석 투표 응답 DTO
 */
data class AttendanceVoteResponse(
    val voteId: Long,
    val gameId: Long,
    val playerId: Long,
    val playerName: String,
    val voteType: VoteType,
    val absenceReason: AbsenceReason?,
    val reasonDetail: String?,
)

/**
 * 출석 투표 요약 응답 DTO
 */
data class AttendanceSummaryResponse(
    val pollId: Long,
    val gameId: Long,
    val totalVotes: Int,
    val attending: Int,
    val absent: Int,
    val undecided: Int,
    val responseRate: Double,
)

/**
 * 미투표자 응답 DTO
 */
data class NonVoterResponse(
    val playerId: Long,
    val playerName: String,
)

/**
 * 투표 현황 전체 응답 DTO
 */
data class AttendanceVotesResponse(
    val gameId: Long,
    val pollId: Long,
    val votes: List<AttendanceVoteResponse>,
    val summary: AttendanceSummaryResponse,
)
