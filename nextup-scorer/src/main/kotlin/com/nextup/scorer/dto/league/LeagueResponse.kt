package com.nextup.scorer.dto.league

import com.nextup.core.domain.league.League
import java.time.Instant

/**
 * 리그 응답 DTO (기록원용)
 */
data class LeagueResponse(
    val id: Long,
    val associationId: Long,
    val associationName: String,
    val name: String,
    val abbreviation: String?,
    val foundedYear: Int,
    val divisionLevel: Int?,
    val description: String?,
    val logoUrl: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(league: League): LeagueResponse {
            return LeagueResponse(
                id = league.id,
                associationId = league.association.id,
                associationName = league.association.name,
                name = league.name,
                abbreviation = league.abbreviation,
                foundedYear = league.foundedYear,
                divisionLevel = league.divisionLevel,
                description = league.description,
                logoUrl = league.logoUrl,
                isActive = league.isActive,
                createdAt = league.createdAt,
                updatedAt = league.updatedAt
            )
        }
    }
}
