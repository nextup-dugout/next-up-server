package com.nextup.api.dto.search

import com.nextup.core.domain.player.Position
import com.nextup.core.service.search.dto.SearchResultDto

/**
 * 통합 검색 응답 DTO
 */
data class SearchResponse(
    val players: List<PlayerSearchResponse>,
    val teams: List<TeamSearchResponse>,
    val competitions: List<CompetitionSearchResponse>,
) {
    companion object {
        fun from(dto: SearchResultDto): SearchResponse =
            SearchResponse(
                players = dto.players.map { PlayerSearchResponse.from(it) },
                teams = dto.teams.map { TeamSearchResponse.from(it) },
                competitions = dto.competitions.map { CompetitionSearchResponse.from(it) },
            )
    }
}

/**
 * 선수 검색 결과 응답 DTO
 */
data class PlayerSearchResponse(
    val playerId: Long,
    val playerName: String,
    val primaryPosition: Position,
    val profileImageUrl: String?,
    val teamName: String?,
) {
    companion object {
        fun from(dto: com.nextup.core.service.search.dto.PlayerSearchDto): PlayerSearchResponse =
            PlayerSearchResponse(
                playerId = dto.playerId,
                playerName = dto.playerName,
                primaryPosition = dto.primaryPosition,
                profileImageUrl = dto.profileImageUrl,
                teamName = dto.teamName,
            )
    }
}

/**
 * 팀 검색 결과 응답 DTO
 */
data class TeamSearchResponse(
    val teamId: Long,
    val teamName: String,
    val city: String,
    val logoUrl: String?,
    val isActive: Boolean,
) {
    companion object {
        fun from(dto: com.nextup.core.service.search.dto.TeamSearchDto): TeamSearchResponse =
            TeamSearchResponse(
                teamId = dto.teamId,
                teamName = dto.teamName,
                city = dto.city,
                logoUrl = dto.logoUrl,
                isActive = dto.isActive,
            )
    }
}

/**
 * 대회 검색 결과 응답 DTO
 */
data class CompetitionSearchResponse(
    val competitionId: Long,
    val competitionName: String,
    val leagueName: String,
    val year: Int,
) {
    companion object {
        fun from(dto: com.nextup.core.service.search.dto.CompetitionSearchDto): CompetitionSearchResponse =
            CompetitionSearchResponse(
                competitionId = dto.competitionId,
                competitionName = dto.competitionName,
                leagueName = dto.leagueName,
                year = dto.year,
            )
    }
}
