package com.nextup.scorer.controller.league

import com.nextup.infrastructure.service.league.LeagueService
import com.nextup.scorer.dto.common.ApiResponse
import com.nextup.scorer.dto.league.CreateLeagueRequest
import com.nextup.scorer.dto.league.LeagueResponse
import com.nextup.scorer.dto.league.UpdateLeagueRequest
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

/**
 * 리그 관리 API Controller (기록원용)
 *
 * 기록원은 본인 협회의 리그만 관리할 수 있습니다.
 * TODO: Spring Security 적용 시 협회 권한 체크 추가
 */
@RestController
@RequestMapping("/api/scorer/leagues")
class LeagueController(
    private val leagueService: LeagueService
) {

    /**
     * 모든 리그 목록을 조회합니다 (비활성화 포함).
     * TODO: 기록원 본인 협회 필터링 적용
     */
    @GetMapping
    fun getAllLeagues(
        @RequestParam(required = false) associationId: Long?
    ): ApiResponse<List<LeagueResponse>> {
        val leagues = if (associationId != null) {
            leagueService.getByAssociationId(associationId)
        } else {
            leagueService.getAll()
        }

        return ApiResponse.success(
            leagues.map { LeagueResponse.from(it) }
        )
    }

    /**
     * 리그 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getLeague(
        @PathVariable id: Long
    ): ApiResponse<LeagueResponse> {
        val league = leagueService.getById(id)
        return ApiResponse.success(LeagueResponse.from(league))
    }

    /**
     * 리그를 생성합니다.
     * TODO: 기록원 본인 협회 권한 체크 적용
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createLeague(
        @Valid @RequestBody request: CreateLeagueRequest
    ): ApiResponse<LeagueResponse> {
        val league = leagueService.create(
            associationId = request.associationId,
            name = request.name,
            abbreviation = request.abbreviation,
            foundedYear = request.foundedYear,
            divisionLevel = request.divisionLevel,
            description = request.description,
            logoUrl = request.logoUrl
        )
        return ApiResponse.success(LeagueResponse.from(league))
    }

    /**
     * 리그 정보를 수정합니다.
     * TODO: 기록원 본인 협회 권한 체크 적용
     */
    @PutMapping("/{id}")
    fun updateLeague(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLeagueRequest
    ): ApiResponse<LeagueResponse> {
        val league = leagueService.update(
            id = id,
            description = request.description,
            logoUrl = request.logoUrl
        )
        return ApiResponse.success(LeagueResponse.from(league))
    }

    /**
     * 리그를 비활성화합니다.
     * TODO: 기록원 본인 협회 권한 체크 적용
     */
    @DeleteMapping("/{id}")
    fun deactivateLeague(
        @PathVariable id: Long
    ): ApiResponse<LeagueResponse> {
        val league = leagueService.deactivate(id)
        return ApiResponse.success(LeagueResponse.from(league))
    }

    /**
     * 리그를 활성화합니다.
     * TODO: 기록원 본인 협회 권한 체크 적용
     */
    @PostMapping("/{id}/activate")
    fun activateLeague(
        @PathVariable id: Long
    ): ApiResponse<LeagueResponse> {
        val league = leagueService.activate(id)
        return ApiResponse.success(LeagueResponse.from(league))
    }
}
