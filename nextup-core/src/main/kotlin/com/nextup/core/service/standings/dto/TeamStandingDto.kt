package com.nextup.core.service.standings.dto

import java.math.BigDecimal

/**
 * 팀 순위 정보 DTO
 */
data class TeamStandingDto(
    val rank: Int,
    val teamId: Long,
    val teamName: String,
    val gamesPlayed: Int,
    val remainingGames: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winningPercentage: BigDecimal,
    val gamesBehind: BigDecimal,
    val runsScored: Int,
    val runsAllowed: Int,
    val runDifferential: Int,
    val logoUrl: String? = null,
    /** 타이브레이커가 적용되었는지 여부 */
    val tiebreakerApplied: Boolean = false,
    /** 타이브레이커 적용 사유 (예: "상대 전적", "득실점차") */
    val tiebreakerReason: String? = null,
)
