package com.nextup.api.dto.attendance

import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.attendance.VoteType
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * 출석 투표 생성 요청 DTO
 */
data class CreatePollRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    @field:NotNull(message = "이벤트 날짜는 필수입니다")
    @field:Future(message = "이벤트 날짜는 미래여야 합니다")
    val eventDate: LocalDateTime,
    @field:NotNull(message = "마감 시간은 필수입니다")
    val deadline: LocalDateTime,
)

/**
 * 출석 투표 응답 DTO
 */
data class PollResponse(
    val id: Long,
    val teamId: Long,
    val title: String,
    val eventDate: LocalDateTime,
    val deadline: LocalDateTime,
    val status: PollStatus,
    val isExpired: Boolean,
    val canVote: Boolean,
    val createdAt: String,
)

/**
 * 투표 제출 요청 DTO
 */
data class SubmitVoteRequest(
    @field:NotNull(message = "선수 ID는 필수입니다")
    val playerId: Long,
    @field:NotNull(message = "투표 유형은 필수입니다")
    val voteType: VoteType,
)

/**
 * 투표 응답 DTO
 */
data class VoteResponse(
    val id: Long,
    val pollId: Long,
    val playerId: Long,
    val playerName: String,
    val voteType: VoteType,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * 투표 통계 DTO
 */
data class PollStatisticsResponse(
    val pollId: Long,
    val totalVotes: Int,
    val attendCount: Int,
    val absentCount: Int,
    val undecidedCount: Int,
)

/**
 * Extension Functions for Mapping
 */
fun AttendancePoll.toResponse(): PollResponse =
    PollResponse(
        id = this.id,
        teamId = this.team.id,
        title = this.title,
        eventDate = this.eventDate,
        deadline = this.deadline,
        status = this.status,
        isExpired = this.isExpired(),
        canVote = this.canVote(),
        createdAt = this.createdAt.toString(),
    )

fun AttendanceVote.toResponse(): VoteResponse =
    VoteResponse(
        id = this.id,
        pollId = this.poll.id,
        playerId = this.player.id,
        playerName = this.player.name,
        voteType = this.voteType,
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString(),
    )

fun List<AttendanceVote>.toStatistics(pollId: Long): PollStatisticsResponse =
    PollStatisticsResponse(
        pollId = pollId,
        totalVotes = this.size,
        attendCount = this.count { it.voteType == VoteType.ATTEND },
        absentCount = this.count { it.voteType == VoteType.ABSENT },
        undecidedCount = this.count { it.voteType == VoteType.UNDECIDED },
    )
