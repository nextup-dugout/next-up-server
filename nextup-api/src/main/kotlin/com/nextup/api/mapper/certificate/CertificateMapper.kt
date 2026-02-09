package com.nextup.api.mapper.certificate

import com.nextup.api.dto.certificate.*
import com.nextup.core.dto.certificate.*

/**
 * CertificateDto를 CertificateResponse로 변환합니다.
 */
fun CertificateDto.toResponse(): CertificateResponse =
    CertificateResponse(
        id = this.id,
        issueNumber = this.issueNumber,
        playerId = this.playerId,
        playerName = this.playerName,
        issuedAt = this.issuedAt,
        expiresAt = this.expiresAt,
        status = this.status,
        playerCareer = this.playerCareer.toResponse(),
        qrCodeData = this.qrCodeData,
    )

/**
 * PlayerCareerDto를 PlayerCareerResponse로 변환합니다.
 */
fun PlayerCareerDto.toResponse(): PlayerCareerResponse =
    PlayerCareerResponse(
        totalGames = this.totalGames,
        battingStats = this.battingStats?.toResponse(),
        pitchingStats = this.pitchingStats?.toResponse(),
        competitionHistory = this.competitionHistory.map { it.toResponse() },
    )

/**
 * CareerBattingStatsDto를 CareerBattingStatsResponse로 변환합니다.
 */
fun CareerBattingStatsDto.toResponse(): CareerBattingStatsResponse =
    CareerBattingStatsResponse(
        games = this.games,
        plateAppearances = this.plateAppearances,
        atBats = this.atBats,
        hits = this.hits,
        doubles = this.doubles,
        triples = this.triples,
        homeRuns = this.homeRuns,
        runsBattedIn = this.runsBattedIn,
        stolenBases = this.stolenBases,
        battingAverage = this.battingAverage,
        onBasePercentage = this.onBasePercentage,
        sluggingPercentage = this.sluggingPercentage,
    )

/**
 * CareerPitchingStatsDto를 CareerPitchingStatsResponse로 변환합니다.
 */
fun CareerPitchingStatsDto.toResponse(): CareerPitchingStatsResponse =
    CareerPitchingStatsResponse(
        games = this.games,
        wins = this.wins,
        losses = this.losses,
        saves = this.saves,
        holds = this.holds,
        inningsPitched = this.inningsPitched,
        strikeouts = this.strikeouts,
        walks = this.walks,
        earnedRuns = this.earnedRuns,
        earnedRunAverage = this.earnedRunAverage,
        walksAndHitsPerInningPitched = this.walksAndHitsPerInningPitched,
    )

/**
 * CompetitionHistoryDto를 CompetitionHistoryResponse로 변환합니다.
 */
fun CompetitionHistoryDto.toResponse(): CompetitionHistoryResponse =
    CompetitionHistoryResponse(
        year = this.year,
        competitionName = this.competitionName,
        teamName = this.teamName,
        games = this.games,
    )

/**
 * CertificateVerificationDto를 CertificateVerificationResponse로 변환합니다.
 */
fun CertificateVerificationDto.toResponse(): CertificateVerificationResponse =
    CertificateVerificationResponse(
        valid = this.valid,
        issueNumber = this.issueNumber,
        playerName = this.playerName,
        issuedAt = this.issuedAt,
        expiresAt = this.expiresAt,
        status = this.status,
    )

/**
 * List<CertificateDto>를 List<CertificateResponse>로 변환합니다.
 */
fun List<CertificateDto>.toResponse(): List<CertificateResponse> = this.map { it.toResponse() }
