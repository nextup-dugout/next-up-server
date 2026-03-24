package com.nextup.backoffice.dto.league

import com.nextup.core.domain.league.League

/**
 * League Entity를 LeagueAdminResponse DTO로 변환하는 Extension Function
 */
fun League.toAdminResponse(): LeagueAdminResponse =
    LeagueAdminResponse(
        id = this.id,
        associationId = this.association.id,
        associationName = this.association.name,
        name = this.name,
        abbreviation = this.abbreviation,
        foundedYear = this.foundedYear,
        divisionLevel = this.divisionLevel,
        description = this.description,
        logoUrl = this.logoUrl,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
