package com.nextup.core.dto.certificate

import com.nextup.core.domain.certificate.CertificateStatus
import java.time.Instant

/**
 * 증명서 발급 요청 DTO
 */
data class IssueCertificateRequest(
    val validityDays: Long = 365,
)

/**
 * 증명서 상세 정보 DTO
 */
data class CertificateDto(
    val id: Long,
    val issueNumber: String,
    val playerId: Long,
    val playerName: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val status: CertificateStatus,
    val playerCareer: PlayerCareerDto,
    val qrCodeData: String,
)

/**
 * 선수 경력 정보 DTO
 */
data class PlayerCareerDto(
    val totalGames: Int,
    val battingStats: CareerBattingStatsDto?,
    val pitchingStats: CareerPitchingStatsDto?,
    val competitionHistory: List<CompetitionHistoryDto>,
)

/**
 * 통산 타격 기록 DTO
 */
data class CareerBattingStatsDto(
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
 * 통산 투수 기록 DTO
 */
data class CareerPitchingStatsDto(
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
 * 대회 이력 DTO
 */
data class CompetitionHistoryDto(
    val year: Int,
    val competitionName: String,
    val teamName: String,
    val games: Int,
)

/**
 * 증명서 검증 응답 DTO
 */
data class CertificateVerificationDto(
    val valid: Boolean,
    val issueNumber: String,
    val playerName: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val status: CertificateStatus,
)
