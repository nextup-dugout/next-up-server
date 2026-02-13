package com.nextup.api.dto.certificate

import com.nextup.core.domain.certificate.CertificateStatus
import java.time.Instant

/**
 * 증명서 발급 요청
 */
data class IssueCertificateRequest(
    val validityDays: Long = 365,
)

/**
 * 증명서 응답
 */
data class CertificateResponse(
    val id: Long,
    val issueNumber: String,
    val playerId: Long,
    val playerName: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val status: CertificateStatus,
    val playerCareer: PlayerCareerResponse,
    val qrCodeData: String,
)

/**
 * 선수 경력 응답
 */
data class PlayerCareerResponse(
    val totalGames: Int,
    val battingStats: CareerBattingStatsResponse?,
    val pitchingStats: CareerPitchingStatsResponse?,
    val competitionHistory: List<CompetitionHistoryResponse>,
)

/**
 * 통산 타격 기록 응답
 */
data class CareerBattingStatsResponse(
    val games: Int,
    val plateAppearances: Int,
    val atBats: Int,
    val hits: Int,
    val doubles: Int,
    val triples: Int,
    val homeRuns: Int,
    val runsBattedIn: Int,
    val stolenBases: Int,
    val battingAverage: Double,
    val onBasePercentage: Double,
    val sluggingPercentage: Double,
)

/**
 * 통산 투수 기록 응답
 */
data class CareerPitchingStatsResponse(
    val games: Int,
    val wins: Int,
    val losses: Int,
    val saves: Int,
    val holds: Int,
    val inningsPitched: Double,
    val strikeouts: Int,
    val walks: Int,
    val earnedRuns: Int,
    val earnedRunAverage: Double,
    val walksAndHitsPerInningPitched: Double,
)

/**
 * 대회 이력 응답
 */
data class CompetitionHistoryResponse(
    val year: Int,
    val competitionName: String,
    val teamName: String,
    val games: Int,
)

/**
 * 증명서 검증 응답
 */
data class CertificateVerificationResponse(
    val valid: Boolean,
    val issueNumber: String,
    val playerName: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val status: CertificateStatus,
)
