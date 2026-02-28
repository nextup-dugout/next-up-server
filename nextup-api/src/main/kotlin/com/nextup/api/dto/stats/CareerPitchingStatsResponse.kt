package com.nextup.api.dto.stats

import java.time.Instant

/**
 * 통산 투수 통계 응답 DTO
 *
 * Entity의 모든 기본 필드와 계산 속성을 포함합니다.
 * BigDecimal은 클라이언트 표시 편의를 위해 String으로 변환됩니다.
 */
data class CareerPitchingStatsResponse(
    // 메타 정보
    val id: Long,
    val playerId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    // 시즌 및 출전 정보
    val seasonsPlayed: Int,
    val gamesPlayed: Int,
    val gamesStarted: Int,
    // 기본 투수 기록
    val inningsPitchedOuts: Int,
    val wins: Int,
    val losses: Int,
    val saves: Int,
    val holds: Int,
    val blownSaves: Int,
    val earnedRuns: Int,
    val runsAllowed: Int,
    val hitsAllowed: Int,
    val walksAllowed: Int,
    val strikeouts: Int,
    val homeRunsAllowed: Int,
    val hitBatsmen: Int,
    val wildPitches: Int,
    val balks: Int,
    val battersFaced: Int,
    val pitchesThrown: Int?, // nullable
    val strikesThrown: Int?, // nullable
    // 계산 속성
    val completeInnings: Int,
    val remainingOuts: Int,
    val inningsPitched: String, // "5.33"
    val inningsPitchedDisplay: String, // "5.1"
    val earnedRunAverage: String?, // "3.50" or null (0이닝 + 자책점 있는 경우 계산 불가)
    val whip: String, // "1.20"
    val strikeoutsPer9: String, // "9.00"
    val walksPer9: String, // "2.50"
    val strikeoutToWalkRatio: String, // "3.60"
    val strikePercentage: String?, // "0.650" (nullable)
    val unearnedRuns: Int,
    val winningPercentage: String, // "0.667"
)
