package com.nextup.api.controller.report

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.report.CompetitionReportResponse
import com.nextup.api.dto.report.CompetitionSummaryResponse
import com.nextup.api.dto.report.toResponse
import com.nextup.core.service.report.CompetitionReportService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 대회 리포트 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/competitions")
class CompetitionReportController(
    private val competitionReportService: CompetitionReportService,
) {
    /**
     * 대회의 전체 리포트를 조회합니다.
     *
     * @param competitionId 대회 ID
     * @return 대회 리포트 (순위표, 요약 통계 포함)
     */
    @GetMapping("/{competitionId}/report")
    fun getReport(
        @PathVariable competitionId: Long,
    ): ApiResponse<CompetitionReportResponse> {
        val report = competitionReportService.getReport(competitionId)
        return ApiResponse.success(report.toResponse())
    }

    /**
     * 대회의 요약 정보만 조회합니다.
     *
     * @param competitionId 대회 ID
     * @return 대회 요약 통계
     */
    @GetMapping("/{competitionId}/report/summary")
    fun getReportSummary(
        @PathVariable competitionId: Long,
    ): ApiResponse<CompetitionSummaryResponse> {
        val summary = competitionReportService.getReportSummary(competitionId)
        return ApiResponse.success(summary.toResponse())
    }
}
