package com.nextup.core.service.report

import com.nextup.core.service.report.dto.CompetitionReportDto
import com.nextup.core.service.report.dto.CompetitionSummaryDto

/**
 * 대회 리포트 서비스
 *
 * 대회의 종합 리포트 및 요약 정보를 제공합니다.
 */
interface CompetitionReportService {
    /**
     * 대회의 전체 리포트를 조회합니다.
     *
     * @param competitionId 대회 ID
     * @return 대회 리포트 (순위표, 요약 통계 포함)
     */
    fun getReport(competitionId: Long): CompetitionReportDto

    /**
     * 대회의 요약 정보만 조회합니다.
     *
     * @param competitionId 대회 ID
     * @return 대회 요약 통계
     */
    fun getReportSummary(competitionId: Long): CompetitionSummaryDto
}
