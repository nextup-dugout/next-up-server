package com.nextup.api.dto.title

import com.nextup.core.service.title.dto.TitleCandidateDto
import com.nextup.core.service.title.dto.TitleDto
import com.nextup.core.service.title.dto.TitleWinnerDto

/**
 * 타이틀 목록 응답
 */
data class TitleListResponse(
    val competitionId: Long,
    val titles: List<TitleResponse>,
)

/**
 * 타이틀 응답
 */
data class TitleResponse(
    val category: String,
    val displayName: String,
    val winner: TitleWinnerResponse?,
    val topCandidates: List<TitleCandidateResponse>,
)

/**
 * 타이틀 수상자 응답
 */
data class TitleWinnerResponse(
    val playerId: Long,
    val playerName: String,
    val teamName: String,
    val statValue: Double,
)

/**
 * 타이틀 후보자 응답
 */
data class TitleCandidateResponse(
    val rank: Int,
    val playerId: Long,
    val playerName: String,
    val teamName: String,
    val statValue: Double,
    val isQualified: Boolean,
)

// ========== Extension Functions (Mappers) ==========

/**
 * TitleDto를 TitleResponse로 변환합니다.
 */
fun TitleDto.toResponse(): TitleResponse =
    TitleResponse(
        category = this.category.name,
        displayName = this.displayName,
        winner = this.winner?.toResponse(),
        topCandidates = this.topCandidates.map { it.toResponse() },
    )

/**
 * TitleWinnerDto를 TitleWinnerResponse로 변환합니다.
 */
fun TitleWinnerDto.toResponse(): TitleWinnerResponse =
    TitleWinnerResponse(
        playerId = this.playerId,
        playerName = this.playerName,
        teamName = this.teamName,
        statValue = this.statValue,
    )

/**
 * TitleCandidateDto를 TitleCandidateResponse로 변환합니다.
 */
fun TitleCandidateDto.toResponse(): TitleCandidateResponse =
    TitleCandidateResponse(
        rank = this.rank,
        playerId = this.playerId,
        playerName = this.playerName,
        teamName = this.teamName,
        statValue = this.statValue,
        isQualified = this.isQualified,
    )

/**
 * List<TitleDto>를 TitleListResponse로 변환합니다.
 */
fun List<TitleDto>.toListResponse(competitionId: Long): TitleListResponse =
    TitleListResponse(
        competitionId = competitionId,
        titles = this.map { it.toResponse() },
    )
