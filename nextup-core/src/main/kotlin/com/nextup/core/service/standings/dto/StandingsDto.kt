package com.nextup.core.service.standings.dto

import java.time.LocalDateTime

/**
 * 순위표 전체 DTO
 */
data class StandingsDto(
    val competitionId: Long,
    val competitionName: String,
    val totalGamesPerTeam: Int,
    val standings: List<TeamStandingDto>,
    val lastUpdated: LocalDateTime,
)
