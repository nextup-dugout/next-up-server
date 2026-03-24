package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.player.Position
import com.nextup.core.service.game.dto.BoxScoreDto
import com.nextup.core.service.game.dto.GameTimelineDto
import com.nextup.core.service.game.dto.TimelineEventDto
import java.time.Instant
import java.time.LocalDateTime

/**
 * 현재 경기 상태 응답 DTO
 *
 * 기록원 재접속 시 현재 경기 상태를 복원하기 위한 응답입니다.
 */
data class CurrentGameStateResponse(
    val gameId: Long,
    val status: GameStatus,
    val currentInning: Int,
    val isTopInning: Boolean,
    val currentInningDisplay: String,
    val totalInnings: Int,
    val gameState: GameStateResponse,
    val homeTeam: TeamScoreSummaryResponse,
    val awayTeam: TeamScoreSummaryResponse,
    val scheduledAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?,
    val timeLimitReached: Boolean = false,
)

/**
 * 팀 점수 요약 응답 DTO
 */
data class TeamScoreSummaryResponse(
    val gameTeamId: Long,
    val teamId: Long,
    val teamName: String,
    val totalScore: Int,
    val totalHits: Int,
    val totalErrors: Int,
)

/**
 * 현재 라인업 응답 DTO
 */
data class CurrentLineupResponse(
    val gameId: Long,
    val homeLineup: List<LineupPlayerResponse>,
    val awayLineup: List<LineupPlayerResponse>,
)

/**
 * 라인업 선수 응답 DTO
 */
data class LineupPlayerResponse(
    val gamePlayerId: Long,
    val playerId: Long,
    val playerName: String,
    val position: Position,
    val positionDisplayName: String,
    val battingOrder: Int?,
    val backNumber: Int?,
    val isStarter: Boolean,
    val isCurrentlyPlaying: Boolean,
    val isDesignatedHitter: Boolean,
)

/**
 * 이벤트 타임라인 응답 DTO
 */
data class EventTimelineResponse(
    val gameId: Long,
    val events: List<TimelineEventResponse>,
    val totalEvents: Int,
)

/**
 * 타임라인 이벤트 응답 DTO
 */
data class TimelineEventResponse(
    val eventId: Long,
    val inning: Int,
    val isTopInning: Boolean,
    val inningDisplay: String,
    val eventType: String,
    val description: String,
    val batterId: Long?,
    val batterName: String?,
    val pitcherId: Long?,
    val pitcherName: String?,
    val plateAppearanceResult: String?,
    val runsScored: Int,
    val outCountBefore: Int,
    val outCountAfter: Int,
    val eventTimestamp: Instant,
)

/**
 * 스코어보드 REST fallback 응답 DTO
 */
data class ScoreboardResponse(
    val gameId: Long,
    val status: GameStatus,
    val currentInning: Int,
    val isTopInning: Boolean,
    val currentInningDisplay: String,
    val homeTeam: ScoreboardTeamResponse,
    val awayTeam: ScoreboardTeamResponse,
)

/**
 * 스코어보드 팀 응답 DTO
 */
data class ScoreboardTeamResponse(
    val teamId: Long,
    val teamName: String,
    val runs: Int,
    val hits: Int,
    val errors: Int,
    val inningScores: List<Int>,
)

// ===== 매퍼 함수 =====

/**
 * Game + GameTeam 목록을 CurrentGameStateResponse로 변환합니다.
 */
fun Game.toCurrentGameStateResponse(gameTeams: List<GameTeam>): CurrentGameStateResponse {
    val homeTeam =
        gameTeams.firstOrNull { it.homeAway == HomeAway.HOME }
    val awayTeam =
        gameTeams.firstOrNull { it.homeAway == HomeAway.AWAY }

    return CurrentGameStateResponse(
        gameId = this.id,
        status = this.status,
        currentInning = this.currentInning,
        isTopInning = this.isTopInning,
        currentInningDisplay = this.currentInningDisplay,
        totalInnings = this.totalInnings,
        gameState = this.gameState.toResponse(),
        homeTeam = homeTeam?.toTeamScoreSummary() ?: emptyTeamScoreSummary(),
        awayTeam = awayTeam?.toTeamScoreSummary() ?: emptyTeamScoreSummary(),
        scheduledAt = this.scheduledAt,
        startedAt = this.startedAt,
        endedAt = this.endedAt,
        timeLimitReached = this.timeLimitReached,
    )
}

private fun GameTeam.toTeamScoreSummary(): TeamScoreSummaryResponse =
    TeamScoreSummaryResponse(
        gameTeamId = this.id,
        teamId = this.team.id,
        teamName = this.team.name,
        totalScore = this.totalScore,
        totalHits = this.totalHits,
        totalErrors = this.totalErrors,
    )

private fun emptyTeamScoreSummary(): TeamScoreSummaryResponse =
    TeamScoreSummaryResponse(
        gameTeamId = 0L,
        teamId = 0L,
        teamName = "",
        totalScore = 0,
        totalHits = 0,
        totalErrors = 0,
    )

/**
 * GamePlayer를 LineupPlayerResponse로 변환합니다.
 */
fun GamePlayer.toLineupPlayerResponse(): LineupPlayerResponse =
    LineupPlayerResponse(
        gamePlayerId = this.id,
        playerId = this.player.id,
        playerName = this.player.name,
        position = this.position,
        positionDisplayName = this.position.displayName,
        battingOrder = this.battingOrder,
        backNumber = this.backNumber,
        isStarter = this.isStarter,
        isCurrentlyPlaying = this.isCurrentlyPlaying,
        isDesignatedHitter = this.isDesignatedHitter,
    )

/**
 * GamePlayer 목록을 홈/원정으로 분리하여 CurrentLineupResponse를 생성합니다.
 */
fun toCurrentLineupResponse(
    gameId: Long,
    players: List<GamePlayer>,
): CurrentLineupResponse {
    val homeLineup =
        players
            .filter { it.gameTeam.homeAway == HomeAway.HOME }
            .sortedWith(compareBy(nullsLast()) { it.battingOrder })
            .map { it.toLineupPlayerResponse() }

    val awayLineup =
        players
            .filter { it.gameTeam.homeAway == HomeAway.AWAY }
            .sortedWith(compareBy(nullsLast()) { it.battingOrder })
            .map { it.toLineupPlayerResponse() }

    return CurrentLineupResponse(
        gameId = gameId,
        homeLineup = homeLineup,
        awayLineup = awayLineup,
    )
}

/**
 * GameTimelineDto를 EventTimelineResponse로 변환합니다.
 */
fun GameTimelineDto.toEventTimelineResponse(): EventTimelineResponse =
    EventTimelineResponse(
        gameId = this.gameId,
        events = this.events.map { it.toTimelineEventResponse() },
        totalEvents = this.totalEvents,
    )

private fun TimelineEventDto.toTimelineEventResponse(): TimelineEventResponse =
    TimelineEventResponse(
        eventId = this.eventId,
        inning = this.inning,
        isTopInning = this.isTopInning,
        inningDisplay = this.inningDisplay,
        eventType = this.eventType,
        description = this.description,
        batterId = this.batterId,
        batterName = this.batterName,
        pitcherId = this.pitcherId,
        pitcherName = this.pitcherName,
        plateAppearanceResult = this.plateAppearanceResult,
        runsScored = this.runsScored,
        outCountBefore = this.outCountBefore,
        outCountAfter = this.outCountAfter,
        eventTimestamp = this.eventTimestamp,
    )

/**
 * Game + GameTeam 목록을 ScoreboardResponse로 변환합니다.
 */
fun BoxScoreDto.toScoreboardResponse(): ScoreboardResponse =
    ScoreboardResponse(
        gameId = this.gameId,
        status = com.nextup.core.domain.game.GameStatus.valueOf(this.gameStatus),
        currentInning =
            this.currentInning
                .replace("회초", "")
                .replace("회말", "")
                .replace("경기 전", "0")
                .toIntOrNull() ?: 0,
        isTopInning = this.currentInning.contains("초"),
        currentInningDisplay = this.currentInning,
        homeTeam =
            ScoreboardTeamResponse(
                teamId = this.homeTeam.teamId,
                teamName = this.homeTeam.teamName,
                runs = this.homeTeam.runs,
                hits = this.homeTeam.hits,
                errors = this.homeTeam.errors,
                inningScores = this.homeTeam.inningScores,
            ),
        awayTeam =
            ScoreboardTeamResponse(
                teamId = this.awayTeam.teamId,
                teamName = this.awayTeam.teamName,
                runs = this.awayTeam.runs,
                hits = this.awayTeam.hits,
                errors = this.awayTeam.errors,
                inningScores = this.awayTeam.inningScores,
            ),
    )

/**
 * Game + GameTeam 목록으로 직접 ScoreboardResponse를 생성합니다.
 */
fun Game.toScoreboardResponse(gameTeams: List<GameTeam>): ScoreboardResponse {
    val homeTeam = gameTeams.firstOrNull { it.homeAway == HomeAway.HOME }
    val awayTeam = gameTeams.firstOrNull { it.homeAway == HomeAway.AWAY }

    return ScoreboardResponse(
        gameId = this.id,
        status = this.status,
        currentInning = this.currentInning,
        isTopInning = this.isTopInning,
        currentInningDisplay = this.currentInningDisplay,
        homeTeam =
            homeTeam?.let {
                ScoreboardTeamResponse(
                    teamId = it.team.id,
                    teamName = it.team.name,
                    runs = it.totalScore,
                    hits = it.totalHits,
                    errors = it.totalErrors,
                    inningScores =
                        (1..maxOf(this.totalInnings, this.currentInning))
                            .map { inning -> it.getInningScore(inning) },
                )
            } ?: ScoreboardTeamResponse(0L, "", 0, 0, 0, emptyList()),
        awayTeam =
            awayTeam?.let {
                ScoreboardTeamResponse(
                    teamId = it.team.id,
                    teamName = it.team.name,
                    runs = it.totalScore,
                    hits = it.totalHits,
                    errors = it.totalErrors,
                    inningScores =
                        (1..maxOf(this.totalInnings, this.currentInning))
                            .map { inning -> it.getInningScore(inning) },
                )
            } ?: ScoreboardTeamResponse(0L, "", 0, 0, 0, emptyList()),
    )
}
