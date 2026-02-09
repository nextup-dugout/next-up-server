package com.nextup.core.service.title.dto

import com.nextup.core.service.title.TitleCategory

/**
 * 타이틀 정보 DTO
 */
data class TitleDto(
    val category: TitleCategory,
    val displayName: String,
    val winner: TitleWinnerDto?,
    val topCandidates: List<TitleCandidateDto>,
)

/**
 * 타이틀 수상자 정보
 */
data class TitleWinnerDto(
    val playerId: Long,
    val playerName: String,
    val teamName: String,
    val statValue: Double,
)

/**
 * 타이틀 후보자 정보
 */
data class TitleCandidateDto(
    val rank: Int,
    val playerId: Long,
    val playerName: String,
    val teamName: String,
    val statValue: Double,
    val isQualified: Boolean,
)
