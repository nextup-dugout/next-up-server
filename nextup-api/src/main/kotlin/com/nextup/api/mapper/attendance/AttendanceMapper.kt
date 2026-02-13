package com.nextup.api.mapper.attendance

import com.nextup.api.dto.attendance.*
import com.nextup.core.domain.game.AttendanceVote
import com.nextup.core.service.game.dto.AttendanceSummaryDto

/**
 * AttendanceVote를 AttendanceVoteResponse로 변환합니다.
 */
fun AttendanceVote.toResponse(): AttendanceVoteResponse =
    AttendanceVoteResponse(
        voteId = this.id,
        gameId = this.game.id,
        memberId = this.member.id,
        status = this.status,
        reason = this.reason,
        respondedAt = this.respondedAt,
    )

/**
 * AttendanceSummaryDto를 AttendanceSummaryResponse로 변환합니다.
 */
fun AttendanceSummaryDto.toResponse(): AttendanceSummaryResponse =
    AttendanceSummaryResponse(
        gameId = this.gameId,
        totalMembers = this.totalMembers,
        attending = this.attending,
        absent = this.absent,
        undecided = this.undecided,
        responseRate = this.responseRate,
    )

/**
 * AttendanceVote를 MemberVoteResponse로 변환합니다.
 */
fun AttendanceVote.toMemberVoteResponse(): MemberVoteResponse =
    MemberVoteResponse(
        voteId = this.id,
        member =
            MemberSummary(
                memberId = this.member.id,
                nickname = this.member.user.nickname,
                uniformNumber = this.member.uniformNumber,
                position = this.member.player.primaryPosition.abbreviation,
            ),
        status = this.status,
        reason = this.reason,
        respondedAt = this.respondedAt,
    )

/**
 * List<AttendanceVote>를 List<AttendanceVoteResponse>로 변환합니다.
 */
fun List<AttendanceVote>.toResponse(): List<AttendanceVoteResponse> = this.map { it.toResponse() }

/**
 * List<AttendanceVote>를 List<MemberVoteResponse>로 변환합니다.
 */
fun List<AttendanceVote>.toMemberVoteResponse(): List<MemberVoteResponse> = this.map { it.toMemberVoteResponse() }
