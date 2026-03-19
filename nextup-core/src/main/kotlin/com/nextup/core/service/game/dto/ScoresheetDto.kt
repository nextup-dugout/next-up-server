package com.nextup.core.service.game.dto

import java.time.LocalDateTime

/**
 * 공식 기록지 데이터
 *
 * 경기의 모든 기록을 포함하는 완전한 데이터 구조
 */
data class ScoresheetDto(
    val gameInfo: GameInfoDto,
    val teams: TeamsScoresheetDto,
    val inningScores: InningScoresDto,
    val battingRecords: BattingRecordsDto,
    val pitchingRecords: PitchingRecordsDto,
    val keyEvents: List<KeyEventDto>,
)

/**
 * 경기 기본 정보
 */
data class GameInfoDto(
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
 * 양 팀 정보
 */
data class TeamsScoresheetDto(
    val home: TeamScoresheetInfoDto,
    val away: TeamScoresheetInfoDto,
)

/**
 * 팀 기록지 정보
 */
data class TeamScoresheetInfoDto(
    val teamId: Long,
    val teamName: String,
    val logoUrl: String?,
    val totalScore: Int,
    val totalHits: Int,
    val totalErrors: Int,
    val result: String,
)

/**
 * 이닝별 점수
 */
data class InningScoresDto(
    val innings: Int,
    val homeScores: List<Int>,
    val awayScores: List<Int>,
)

/**
 * 타격 기록
 */
data class BattingRecordsDto(
    val home: List<BatterScoresheetDto>,
    val away: List<BatterScoresheetDto>,
)

/**
 * 타자 기록지 정보
 */
data class BatterScoresheetDto(
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
 * 투수 기록
 */
data class PitchingRecordsDto(
    val home: List<PitcherScoresheetDto>,
    val away: List<PitcherScoresheetDto>,
)

/**
 * 투수 기록지 정보
 */
data class PitcherScoresheetDto(
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
 * 주요 이벤트
 */
data class KeyEventDto(
    val inning: String,
    val description: String,
    val timestamp: String,
)
