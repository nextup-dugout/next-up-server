package com.nextup.backoffice.controller.competition

import com.nextup.backoffice.dto.competition.CompetitionAdminResponse
import com.nextup.backoffice.dto.competition.CreateCompetitionRequest
import com.nextup.backoffice.dto.competition.PrepareNextSeasonRequest
import com.nextup.backoffice.dto.competition.UpdateCompetitionRequest
import com.nextup.backoffice.dto.competition.WithdrawTeamRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.service.competition.CompetitionService
import com.nextup.core.service.competition.SeasonTransitionService
import com.nextup.core.service.competition.dto.NextSeasonPreparationResult
import com.nextup.core.service.competition.dto.SeasonSummaryDto
import com.nextup.core.service.competition.dto.TeamWithdrawalResult
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 대회 관리 API Controller (관리자용)
 *
 * 전체 권한: 생성, 조회, 수정, 삭제, 상태 변경
 */
@RestController
@RequestMapping("/api/backoffice/competitions")
class CompetitionAdminController(
    private val competitionService: CompetitionService,
    private val seasonTransitionService: SeasonTransitionService,
) {
    /**
     * 모든 대회 목록을 조회합니다.
     */
    @GetMapping
    fun getAllCompetitions(
        @RequestParam(required = false) status: CompetitionStatus?,
    ): ApiResponse<List<CompetitionAdminResponse>> {
        val competitions =
            if (status != null) {
                competitionService.getByStatus(status)
            } else {
                competitionService.getAll()
            }
        return ApiResponse.success(
            competitions.map { CompetitionAdminResponse.from(it) },
        )
    }

    /**
     * 대회 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getCompetition(
        @PathVariable id: Long,
    ): ApiResponse<CompetitionAdminResponse> {
        val competition = competitionService.getByIdWithLeague(id)
        return ApiResponse.success(CompetitionAdminResponse.from(competition))
    }

    /**
     * 리그별 대회 목록을 조회합니다.
     */
    @GetMapping("/by-league/{leagueId}")
    fun getCompetitionsByLeague(
        @PathVariable leagueId: Long,
    ): ApiResponse<List<CompetitionAdminResponse>> {
        val competitions = competitionService.getByLeagueId(leagueId)
        return ApiResponse.success(
            competitions.map { CompetitionAdminResponse.from(it) },
        )
    }

    /**
     * 대회를 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCompetition(
        @Valid @RequestBody request: CreateCompetitionRequest,
    ): ApiResponse<CompetitionAdminResponse> {
        val competition =
            competitionService.create(
                leagueId = request.leagueId,
                name = request.name,
                year = request.year,
                season = request.season,
                type = request.type,
                startDate = request.startDate,
                endDate = request.endDate,
                description = request.description,
                maxTeams = request.maxTeams,
            )
        return ApiResponse.success(CompetitionAdminResponse.from(competition))
    }

    /**
     * 대회 정보를 수정합니다.
     */
    @PutMapping("/{id}")
    fun updateCompetition(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCompetitionRequest,
    ): ApiResponse<CompetitionAdminResponse> {
        val competition =
            competitionService.update(
                id = id,
                description = request.description,
                endDate = request.endDate,
            )
        return ApiResponse.success(CompetitionAdminResponse.from(competition))
    }

    /**
     * 대회를 시작합니다.
     */
    @PostMapping("/{id}/start")
    fun startCompetition(
        @PathVariable id: Long,
    ): ApiResponse<CompetitionAdminResponse> {
        val competition = competitionService.start(id)
        return ApiResponse.success(CompetitionAdminResponse.from(competition))
    }

    /**
     * 대회를 완료합니다.
     */
    @PostMapping("/{id}/complete")
    fun completeCompetition(
        @PathVariable id: Long,
        @RequestBody(required = false) endDate: LocalDate?,
    ): ApiResponse<CompetitionAdminResponse> {
        val competition = competitionService.complete(id, endDate ?: LocalDate.now())
        return ApiResponse.success(CompetitionAdminResponse.from(competition))
    }

    /**
     * 대회를 취소합니다.
     */
    @DeleteMapping("/{id}")
    fun cancelCompetition(
        @PathVariable id: Long,
    ): ApiResponse<CompetitionAdminResponse> {
        val competition = competitionService.cancel(id)
        return ApiResponse.success(CompetitionAdminResponse.from(competition))
    }

    /**
     * 대회를 연기합니다.
     */
    @PostMapping("/{id}/postpone")
    fun postponeCompetition(
        @PathVariable id: Long,
    ): ApiResponse<CompetitionAdminResponse> {
        val competition = competitionService.postpone(id)
        return ApiResponse.success(CompetitionAdminResponse.from(competition))
    }

    /**
     * 대회에서 팀을 전체 탈퇴시킵니다.
     *
     * 잔여 경기 몰수승 처리, 선수 일괄 WITHDRAWN, 대진표 업데이트를 수행합니다.
     */
    @PostMapping("/{id}/teams/{teamId}/withdraw")
    fun withdrawTeam(
        @PathVariable id: Long,
        @PathVariable teamId: Long,
        @Valid @RequestBody request: WithdrawTeamRequest,
    ): ApiResponse<TeamWithdrawalResult> {
        val result = competitionService.withdrawTeam(id, teamId, request.reason)
        return ApiResponse.success(result)
    }

    /**
     * 완료된 대회의 시즌 요약을 조회합니다.
     *
     * 최종 순위, 참가 팀/선수 통계를 반환합니다.
     */
    @GetMapping("/{id}/season-summary")
    fun getSeasonSummary(
        @PathVariable id: Long,
    ): ApiResponse<SeasonSummaryDto> {
        val summary = seasonTransitionService.getSeasonSummary(id)
        return ApiResponse.success(summary)
    }

    /**
     * 완료된 대회를 기반으로 다음 시즌 대회를 준비합니다.
     *
     * 이전 시즌 활성 선수를 자동 등록합니다.
     */
    @PostMapping("/{id}/prepare-next-season")
    @ResponseStatus(HttpStatus.CREATED)
    fun prepareNextSeason(
        @PathVariable id: Long,
        @Valid @RequestBody request: PrepareNextSeasonRequest,
    ): ApiResponse<NextSeasonPreparationResult> {
        val result =
            seasonTransitionService.prepareNextSeason(
                previousCompetitionId = id,
                name = request.name,
                startDate = request.startDate,
                endDate = request.endDate,
                description = request.description,
                maxTeams = request.maxTeams,
            )
        return ApiResponse.success(result)
    }
}
