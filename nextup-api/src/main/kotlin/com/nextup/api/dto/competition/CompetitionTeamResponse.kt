package com.nextup.api.dto.competition

import com.nextup.core.domain.team.Team

/**
 * 대회 참가 팀 응답 DTO
 *
 * 대회에 등록된 팀 정보를 제공합니다.
 */
data class CompetitionTeamResponse(
    val teamId: Long,
    val name: String,
    val city: String,
    val abbreviation: String?,
    val logoUrl: String?,
    val primaryColor: String?,
    val playerCount: Int,
) {
    companion object {
        fun from(
            team: Team,
            playerCount: Int,
        ): CompetitionTeamResponse =
            CompetitionTeamResponse(
                teamId = team.id,
                name = team.name,
                city = team.city,
                abbreviation = team.abbreviation,
                logoUrl = team.logoUrl,
                primaryColor = team.primaryColor,
                playerCount = playerCount,
            )
    }
}
