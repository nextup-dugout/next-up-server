package com.nextup.core.service.game.dto

import com.nextup.core.domain.game.GameStatus
import java.time.LocalDateTime

/**
 * 경기 일정 요약 DTO
 */
data class GameSummaryDto(
    val gameId: Long,
    val competitionId: Long,
    val competitionName: String,
    val homeTeamId: Long,
    val homeTeamName: String,
    val awayTeamId: Long,
    val awayTeamName: String,
    val scheduledAt: LocalDateTime,
    val status: GameStatus,
    val homeScore: Int,
    val awayScore: Int,
    val location: String?,
    val fieldName: String?,
)

/**
 * 경기 상세 DTO
 */
data class GameDetailDto(
    val gameId: Long,
    val competitionId: Long,
    val competitionName: String,
    val homeTeamId: Long,
    val homeTeamName: String,
    val awayTeamId: Long,
    val awayTeamName: String,
    val scheduledAt: LocalDateTime,
    val status: GameStatus,
    val homeScore: Int,
    val awayScore: Int,
    val location: String?,
    val fieldName: String?,
    val gameNumber: Int?,
    val currentInning: String,
    val totalInnings: Int,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?,
    val note: String?,
    val forfeitReason: String?,
)
