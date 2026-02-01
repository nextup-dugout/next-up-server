package com.nextup.api.dto.stats

import java.time.Instant

/**
 * 통산 타격 통계 응답 DTO
 *
 * Entity의 모든 기본 필드와 계산 속성을 포함합니다.
 * BigDecimal은 클라이언트 표시 편의를 위해 String으로 변환됩니다.
 */
data class CareerBattingStatsResponse(
    // 메타 정보
    val id: Long,
    val playerId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,

    // 시즌 및 출전 정보
    val seasonsPlayed: Int,
    val gamesPlayed: Int,

    // 기본 타격 기록
    val plateAppearances: Int,
    val atBats: Int,
    val hits: Int,
    val doubles: Int,
    val triples: Int,
    val homeRuns: Int,
    val runs: Int,
    val runsBattedIn: Int,
    val walks: Int,
    val intentionalWalks: Int,
    val hitByPitch: Int,
    val strikeouts: Int,
    val sacrificeBunts: Int,
    val sacrificeFlies: Int,
    val stolenBases: Int,
    val caughtStealing: Int,
    val groundedIntoDoublePlays: Int,

    // 계산 속성
    val singles: Int,
    val totalBases: Int,
    val extraBaseHits: Int,
    val sacrifices: Int,
    val totalWalks: Int,

    // 비율 지표 (String으로 변환, 소수점 3자리)
    val battingAverage: String,       // "0.300"
    val onBasePercentage: String,     // "0.400"
    val sluggingPercentage: String,   // "0.500"
    val ops: String,                  // "0.900"
    val stolenBasePercentage: String  // "0.750"
)
