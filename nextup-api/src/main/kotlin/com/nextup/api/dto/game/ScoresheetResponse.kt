package com.nextup.api.dto.game

import java.time.LocalDateTime

/**
 * 공식 기록지 응답 DTO
 */
data class ScoresheetResponse(
    val gameInfo: GameInfoResponse,
    val teams: TeamsResponse,
    val inningScores: InningScoresResponse,
    val battingRecords: BattingRecordsResponse,
    val pitchingRecords: PitchingRecordsResponse,
    val keyEvents: List<KeyEventResponse>,
)

/**
 * 경기 기본 정보 응답
 */
data class GameInfoResponse(
    val gameId: Long,
    val competitionName: String,
    val gameNumber: Int?,
    val scheduledAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?,
    val location: String?,
    val fieldName: String?,
    val status: String,
    val currentInning: String,
    val totalInnings: Int,
)

/**
 * 양 팀 정보 응답
 */
data class TeamsResponse(
    val home: TeamScoresheetResponse,
    val away: TeamScoresheetResponse,
)

/**
 * 팀 기록지 정보 응답
 */
data class TeamScoresheetResponse(
    val teamId: Long,
    val teamName: String,
    val totalScore: Int,
    val totalHits: Int,
    val totalErrors: Int,
    val result: String,
)

/**
 * 이닝별 점수 응답
 */
data class InningScoresResponse(
    val innings: Int,
    val homeScores: List<Int>,
    val awayScores: List<Int>,
)

/**
 * 타격 기록 응답
 */
data class BattingRecordsResponse(
    val home: List<BatterScoresheetResponse>,
    val away: List<BatterScoresheetResponse>,
)

/**
 * 타자 기록지 정보 응답
 */
data class BatterScoresheetResponse(
    val playerId: Long,
    val name: String,
    val backNumber: Int?,
    val position: String,
    val battingOrder: Int?,
    val plateAppearances: Int,
    val atBats: Int,
    val runs: Int,
    val hits: Int,
    val doubles: Int,
    val triples: Int,
    val homeRuns: Int,
    val rbis: Int,
    val walks: Int,
    val strikeouts: Int,
    val stolenBases: Int,
    val avg: String,
)

/**
 * 투수 기록 응답
 */
data class PitchingRecordsResponse(
    val home: List<PitcherScoresheetResponse>,
    val away: List<PitcherScoresheetResponse>,
)

/**
 * 투수 기록지 정보 응답
 */
data class PitcherScoresheetResponse(
    val playerId: Long,
    val name: String,
    val backNumber: Int?,
    val isStartingPitcher: Boolean,
    val inningsPitched: String,
    val hitsAllowed: Int,
    val runsAllowed: Int,
    val earnedRuns: Int,
    val walks: Int,
    val strikeouts: Int,
    val homeRunsAllowed: Int,
    val decision: String?,
    val era: String,
)

/**
 * 주요 이벤트 응답
 */
data class KeyEventResponse(
    val inning: String,
    val description: String,
    val timestamp: String,
)
