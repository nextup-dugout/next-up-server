package com.nextup.api.dto.game

import com.nextup.core.domain.game.GameStatus
import com.nextup.core.service.game.dto.GameDetailDto
import com.nextup.core.service.game.dto.GameSummaryDto
import java.time.LocalDateTime

/**
 * 경기 요약 응답 DTO
 */
data class GameSummaryResponse(
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
) {
    companion object {
        fun from(dto: GameSummaryDto): GameSummaryResponse =
            GameSummaryResponse(
                gameId = dto.gameId,
                competitionId = dto.competitionId,
                competitionName = dto.competitionName,
                homeTeam =
                    GameTeamSummary(
                        teamId = dto.homeTeamId,
                        teamName = dto.homeTeamName,
                        score = dto.homeScore,
                    ),
                awayTeam =
                    GameTeamSummary(
                        teamId = dto.awayTeamId,
                        teamName = dto.awayTeamName,
                        score = dto.awayScore,
                    ),
                scheduledAt = dto.scheduledAt,
                status = dto.status,
                statusDisplayName = dto.status.displayName,
                location = dto.location,
                fieldName = dto.fieldName,
            )
    }
}

/**
 * 경기 참여 팀 요약
 */
data class GameTeamSummary(
    val teamId: Long,
    val teamName: String,
    val score: Int,
)

/**
 * 경기 상세 응답 DTO
 */
data class GameDetailResponse(
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
) {
    companion object {
        fun from(dto: GameDetailDto): GameDetailResponse =
            GameDetailResponse(
                gameId = dto.gameId,
                competitionId = dto.competitionId,
                competitionName = dto.competitionName,
                homeTeam =
                    GameTeamSummary(
                        teamId = dto.homeTeamId,
                        teamName = dto.homeTeamName,
                        score = dto.homeScore,
                    ),
                awayTeam =
                    GameTeamSummary(
                        teamId = dto.awayTeamId,
                        teamName = dto.awayTeamName,
                        score = dto.awayScore,
                    ),
                scheduledAt = dto.scheduledAt,
                status = dto.status,
                statusDisplayName = dto.status.displayName,
                location = dto.location,
                fieldName = dto.fieldName,
                gameNumber = dto.gameNumber,
                currentInning = dto.currentInning,
                totalInnings = dto.totalInnings,
                startedAt = dto.startedAt,
                endedAt = dto.endedAt,
                note = dto.note,
                forfeitReason = dto.forfeitReason,
            )
    }
}
