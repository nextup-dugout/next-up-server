package com.nextup.api.dto.game

import com.nextup.core.domain.game.GameStatus
import java.time.LocalDateTime

/**
 * 경기 상세 통합 응답 DTO
 *
 * GET /api/v1/games/{gameId}/aggregate
 */
data class GameAggregateResponse(
    val gameInfo: GameAggregateInfoResponse,
    val boxScore: BoxScoreResponse?,
    val timeline: GameTimelineResponse,
    val scoresheet: ScoresheetResponse?,
)

/**
 * 경기 기본 정보 응답 (통합 API용)
 */
data class GameAggregateInfoResponse(
    val gameId: Long,
    val competitionId: Long,
    val competitionName: String,
    val homeTeam: GameTeamSummary,
    val awayTeam: GameTeamSummary,
    val scheduledAt: LocalDateTime,
    val status: GameStatus,
    val statusDisplayName: String,
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
