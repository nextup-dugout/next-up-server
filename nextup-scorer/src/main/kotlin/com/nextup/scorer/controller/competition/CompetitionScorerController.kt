package com.nextup.scorer.controller.competition

import com.nextup.infrastructure.service.competition.CompetitionService
import com.nextup.scorer.dto.common.ApiResponse
import com.nextup.scorer.dto.competition.CompetitionScorerResponse
import com.nextup.scorer.dto.competition.CreateCompetitionRequest
import com.nextup.scorer.dto.competition.UpdateCompetitionRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 대회 관리 API Controller (기록원용)
 *
 * 대회 생성, 수정, 삭제 및 상태 변경 권한
 */
@RestController
@RequestMapping("/api/scorer/competitions")
class CompetitionScorerController(
    private val competitionService: CompetitionService
) {

    /**
     * 모든 대회 목록을 조회합니다.
     */
    @GetMapping
    fun getAllCompetitions(): ApiResponse<List<CompetitionScorerResponse>> {
        val competitions = competitionService.getAll()
        return ApiResponse.success(
            competitions.map { CompetitionScorerResponse.from(it) }
        )
    }

    /**
     * 대회 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getCompetition(
        @PathVariable id: Long
    ): ApiResponse<CompetitionScorerResponse> {
        val competition = competitionService.getByIdWithLeague(id)
        return ApiResponse.success(CompetitionScorerResponse.from(competition))
    }

    /**
     * 리그별 대회 목록을 조회합니다.
     */
    @GetMapping("/by-league/{leagueId}")
    fun getCompetitionsByLeague(
        @PathVariable leagueId: Long
    ): ApiResponse<List<CompetitionScorerResponse>> {
        val competitions = competitionService.getByLeagueId(leagueId)
        return ApiResponse.success(
            competitions.map { CompetitionScorerResponse.from(it) }
        )
    }

    /**
     * 대회를 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCompetition(
        @Valid @RequestBody request: CreateCompetitionRequest
    ): ApiResponse<CompetitionScorerResponse> {
        val competition = competitionService.create(
            leagueId = request.leagueId,
            name = request.name,
            year = request.year,
            season = request.season,
            type = request.type,
            startDate = request.startDate,
            endDate = request.endDate,
            description = request.description,
            maxTeams = request.maxTeams
        )
        return ApiResponse.success(CompetitionScorerResponse.from(competition))
    }

    /**
     * 대회 정보를 수정합니다.
     */
    @PutMapping("/{id}")
    fun updateCompetition(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCompetitionRequest
    ): ApiResponse<CompetitionScorerResponse> {
        val competition = competitionService.update(
            id = id,
            description = request.description,
            endDate = request.endDate
        )
        return ApiResponse.success(CompetitionScorerResponse.from(competition))
    }

    /**
     * 대회를 시작합니다.
     */
    @PostMapping("/{id}/start")
    fun startCompetition(
        @PathVariable id: Long
    ): ApiResponse<CompetitionScorerResponse> {
        val competition = competitionService.start(id)
        return ApiResponse.success(CompetitionScorerResponse.from(competition))
    }

    /**
     * 대회를 완료합니다.
     */
    @PostMapping("/{id}/complete")
    fun completeCompetition(
        @PathVariable id: Long,
        @RequestBody(required = false) endDate: LocalDate?
    ): ApiResponse<CompetitionScorerResponse> {
        val competition = competitionService.complete(id, endDate ?: LocalDate.now())
        return ApiResponse.success(CompetitionScorerResponse.from(competition))
    }

    /**
     * 대회를 취소합니다.
     */
    @DeleteMapping("/{id}")
    fun cancelCompetition(
        @PathVariable id: Long
    ): ApiResponse<CompetitionScorerResponse> {
        val competition = competitionService.cancel(id)
        return ApiResponse.success(CompetitionScorerResponse.from(competition))
    }

    /**
     * 대회를 연기합니다.
     */
    @PostMapping("/{id}/postpone")
    fun postponeCompetition(
        @PathVariable id: Long
    ): ApiResponse<CompetitionScorerResponse> {
        val competition = competitionService.postpone(id)
        return ApiResponse.success(CompetitionScorerResponse.from(competition))
    }
}
