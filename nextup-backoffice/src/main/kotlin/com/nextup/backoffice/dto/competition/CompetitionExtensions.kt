package com.nextup.backoffice.dto.competition

import com.nextup.core.domain.competition.Competition

/**
 * Competition Entity를 CompetitionAdminResponse DTO로 변환하는 Extension Function
 */
fun Competition.toAdminResponse(): CompetitionAdminResponse =
    CompetitionAdminResponse(
        id = this.id,
        leagueId = this.league.id,
        leagueName = this.league.name,
        leagueAbbreviation = this.league.abbreviation,
        name = this.name,
        year = this.year,
        season = this.season,
        type = this.type,
        startDate = this.startDate,
        endDate = this.endDate,
        status = this.status,
        description = this.description,
        maxTeams = this.maxTeams,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
