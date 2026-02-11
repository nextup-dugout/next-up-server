package com.nextup.core.service.report.dto

import com.nextup.core.service.standings.dto.TeamStandingDto

/**
 * 대회 리포트 DTO
 *
 * 대회의 순위표, 주요 통계, 요약 정보를 포함합니다.
 */
data class CompetitionReportDto(
    val competitionId: Long,
    val competitionName: String,
    val season: Int,
    val standings: List<TeamStandingDto>,
    val summary: CompetitionSummaryDto,
)
