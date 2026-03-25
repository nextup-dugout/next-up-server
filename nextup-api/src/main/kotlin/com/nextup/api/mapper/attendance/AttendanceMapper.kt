package com.nextup.api.mapper.attendance

import com.nextup.api.dto.attendance.*
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.service.attendance.GameVoteSummary

/**
 * AttendanceVote를 게임 투표 응답 DTO로 변환합니다.
 */
fun AttendanceVote.toGameVoteResponse(gameId: Long): AttendanceVoteResponse =
    AttendanceVoteResponse(
        voteId = this.id,
        gameId = gameId,
        playerId = this.player.id,
        playerName = this.player.name,
        voteType = this.voteType,
        absenceReason = this.absenceReason,
        reasonDetail = this.reasonDetail,
    )

/**
 * List<AttendanceVote>를 게임 투표 응답 DTO 리스트로 변환합니다.
 */
fun List<AttendanceVote>.toGameVoteResponse(gameId: Long): List<AttendanceVoteResponse> =
    this.map { it.toGameVoteResponse(gameId) }

/**
 * GameVoteSummary를 AttendanceSummaryResponse로 변환합니다.
 */
fun GameVoteSummary.toSummaryResponse(): AttendanceSummaryResponse =
    AttendanceSummaryResponse(
        pollId = this.pollId,
        gameId = this.gameId,
        totalVotes = this.totalVotes,
        attending = this.attending,
        absent = this.absent,
        undecided = this.undecided,
        responseRate = this.responseRate,
    )
