package com.nextup.api.dto.report

import com.nextup.api.dto.standings.TeamStandingResponse

/**
 * 대회 리포트 응답 DTO
 */
data class CompetitionReportResponse(
    val competitionId: Long,
    val competitionName: String,
    val season: Int,
    val standings: List<TeamStandingResponse>,
    val summary: CompetitionSummaryResponse,
)
