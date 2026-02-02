package com.nextup.backoffice.dto.league

import com.nextup.core.domain.league.League
import java.time.Instant

/**
 * 리그 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 */
data class LeagueAdminResponse(
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
        fun from(league: League): LeagueAdminResponse {
            return LeagueAdminResponse(
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
