package com.nextup.scorer.dto.websocket

/**
 * 스코어보드 메시지 DTO
 *
 * 경기 전체 스코어보드 상태를 브로드캐스트합니다.
 * Topic: /topic/games/{gameId}/scoreboard
 */
data class ScoreboardMessage(
    val gameId: Long,
    val homeTeam: TeamScoreDto,
    val awayTeam: TeamScoreDto,
    val inningScores: InningScoresDto,
    val currentInning: Int,
    val isTopInning: Boolean
)

/**
 * 팀 점수 DTO
 */
data class TeamScoreDto(
    val teamId: Long,
    val teamName: String,
    val logoUrl: String?,
    val runs: Int,
    val hits: Int,
    val errors: Int
)

/**
 * 이닝별 점수 DTO
 */
data class InningScoresDto(
    val homeScores: List<Int>,
    val awayScores: List<Int>
)
