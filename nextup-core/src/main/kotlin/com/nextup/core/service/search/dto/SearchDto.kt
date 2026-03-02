package com.nextup.core.service.search.dto

import com.nextup.core.domain.player.Position

/**
 * 통합 검색 결과 DTO
 */
data class SearchResultDto(
    val players: List<PlayerSearchDto>,
    val teams: List<TeamSearchDto>,
    val competitions: List<CompetitionSearchDto>,
)

/**
 * 선수 검색 결과 DTO
 */
data class PlayerSearchDto(
    val playerId: Long,
    val playerName: String,
    val primaryPosition: Position,
    val profileImageUrl: String?,
    val teamName: String?,
)

/**
 * 팀 검색 결과 DTO
 */
data class TeamSearchDto(
    val teamId: Long,
    val teamName: String,
    val city: String,
    val logoUrl: String?,
    val isActive: Boolean,
)

/**
 * 대회 검색 결과 DTO
 */
data class CompetitionSearchDto(
    val competitionId: Long,
    val competitionName: String,
    val leagueName: String,
    val year: Int,
)
