package com.nextup.api.mapper.attendance

import com.nextup.api.dto.attendance.*
import com.nextup.core.domain.game.GameParticipation
import com.nextup.core.service.game.dto.AttendanceSummaryDto

/**
 * GameParticipation를 AttendanceVoteResponse로 변환합니다.
 */
fun GameParticipation.toResponse(): AttendanceVoteResponse =
    AttendanceVoteResponse(
        voteId = this.id,
        gameId = this.game.id,
        memberId = this.member.id,
        status = this.status,
        absenceReason = this.absenceReason,
        reasonDetail = this.reasonDetail,
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
 * GameParticipation를 MemberVoteResponse로 변환합니다.
 */
fun GameParticipation.toMemberVoteResponse(): MemberVoteResponse =
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
        absenceReason = this.absenceReason,
        reasonDetail = this.reasonDetail,
        respondedAt = this.respondedAt,
    )

/**
 * List<GameParticipation>를 List<AttendanceVoteResponse>로 변환합니다.
 */
fun List<GameParticipation>.toResponse(): List<AttendanceVoteResponse> = this.map { it.toResponse() }

/**
 * List<GameParticipation>를 List<MemberVoteResponse>로 변환합니다.
 */
fun List<GameParticipation>.toMemberVoteResponse(): List<MemberVoteResponse> = this.map { it.toMemberVoteResponse() }
