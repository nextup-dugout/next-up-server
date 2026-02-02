package com.nextup.scorer.dto.competition

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import java.time.Instant
import java.time.LocalDate

/**
 * 대회 응답 DTO (Scorer용)
 *
 * 기록원이 필요한 대회 관리 정보 포함
 */
data class CompetitionScorerResponse(
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
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(competition: Competition): CompetitionScorerResponse {
            return CompetitionScorerResponse(
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
                isActive = competition.isActive,
                createdAt = competition.createdAt,
                updatedAt = competition.updatedAt
            )
        }
    }
}
