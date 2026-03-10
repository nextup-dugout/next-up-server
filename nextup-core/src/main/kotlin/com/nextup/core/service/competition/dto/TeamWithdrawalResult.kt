package com.nextup.core.service.competition.dto

/**
 * 팀 대회 탈퇴 처리 결과
 */
data class TeamWithdrawalResult(
    val competitionId: Long,
    val teamId: Long,
    val teamName: String,
    val withdrawnPlayerCount: Int,
    val forfeitedGameCount: Int,
    val updatedBracketEntryCount: Int,
    val reason: String,
)
