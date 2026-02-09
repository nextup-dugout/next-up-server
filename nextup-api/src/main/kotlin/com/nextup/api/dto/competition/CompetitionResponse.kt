package com.nextup.api.dto.competition

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import java.time.LocalDate

/**
 * 대회 응답 DTO (API - 조회 전용)
 *
 * 일반 사용자용 대회 정보
 */
data class CompetitionResponse(
    val id: Long,
    val leagueId: Long,
    val leagueName: String,
    val name: String,
    val year: Int,
    val season: Int,
    val type: CompetitionType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: CompetitionStatus,
    val description: String?,
    val maxTeams: Int?,
    val playoffTeams: Int?,
) {
    companion object {
        fun from(competition: Competition): CompetitionResponse =
            CompetitionResponse(
                id = competition.id,
                leagueId = competition.league.id,
                leagueName = competition.league.name,
                name = competition.name,
                year = competition.year,
                season = competition.season,
                type = competition.type,
                startDate = competition.startDate,
                endDate = competition.endDate,
                status = competition.status,
                description = competition.description,
                maxTeams = competition.maxTeams,
                playoffTeams = competition.playoffTeams,
            )
    }
}
