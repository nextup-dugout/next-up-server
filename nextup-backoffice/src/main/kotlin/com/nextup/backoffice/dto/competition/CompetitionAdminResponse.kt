package com.nextup.backoffice.dto.competition

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import java.time.Instant
import java.time.LocalDate

/**
 * 대회 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 */
data class CompetitionAdminResponse(
    val id: Long,
    val leagueId: Long,
    val leagueName: String,
    val leagueAbbreviation: String?,
    val name: String,
    val year: Int,
    val season: Int,
    val type: CompetitionType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: CompetitionStatus,
    val description: String?,
    val maxTeams: Int?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(competition: Competition): CompetitionAdminResponse {
            return CompetitionAdminResponse(
                id = competition.id,
                leagueId = competition.league.id,
                leagueName = competition.league.name,
                leagueAbbreviation = competition.league.abbreviation,
                name = competition.name,
                year = competition.year,
                season = competition.season,
                type = competition.type,
                startDate = competition.startDate,
                endDate = competition.endDate,
                status = competition.status,
                description = competition.description,
                maxTeams = competition.maxTeams,
                isActive = competition.isActive,
                createdAt = competition.createdAt,
                updatedAt = competition.updatedAt
            )
        }
    }
}
