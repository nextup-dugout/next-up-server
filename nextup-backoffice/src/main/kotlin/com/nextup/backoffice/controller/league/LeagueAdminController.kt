package com.nextup.backoffice.controller.league

import com.nextup.backoffice.dto.common.ApiResponse
import com.nextup.backoffice.dto.league.CreateLeagueRequest
import com.nextup.backoffice.dto.league.LeagueAdminResponse
import com.nextup.backoffice.dto.league.UpdateLeagueRequest
import com.nextup.core.service.league.LeagueService
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

/**
 * 리그 관리 API Controller (관리자용)
 */
@RestController
@RequestMapping("/api/backoffice/leagues")
class LeagueAdminController(
    private val leagueService: LeagueService,
) {
    /**
     * 모든 리그 목록을 조회합니다 (비활성화 포함).
     */
    @GetMapping
    fun getAllLeagues(): ApiResponse<List<LeagueAdminResponse>> {
        val leagues = leagueService.getAll()
        return ApiResponse.success(
            leagues.map { LeagueAdminResponse.from(it) },
        )
    }

    /**
     * 리그 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getLeague(
        @PathVariable id: Long,
    ): ApiResponse<LeagueAdminResponse> {
        val league = leagueService.getById(id)
        return ApiResponse.success(LeagueAdminResponse.from(league))
    }

    /**
     * 리그를 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createLeague(
        @Valid @RequestBody request: CreateLeagueRequest,
    ): ApiResponse<LeagueAdminResponse> {
        val league =
            leagueService.create(
                associationId = request.associationId,
                name = request.name,
                abbreviation = request.abbreviation,
                foundedYear = request.foundedYear,
                divisionLevel = request.divisionLevel,
                description = request.description,
                logoUrl = request.logoUrl,
            )
        return ApiResponse.success(LeagueAdminResponse.from(league))
    }

    /**
     * 리그 정보를 수정합니다.
     */
    @PutMapping("/{id}")
    fun updateLeague(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLeagueRequest,
    ): ApiResponse<LeagueAdminResponse> {
        val league =
            leagueService.update(
                id = id,
                description = request.description,
                logoUrl = request.logoUrl,
            )
        return ApiResponse.success(LeagueAdminResponse.from(league))
    }

    /**
     * 리그를 비활성화합니다.
     */
    @DeleteMapping("/{id}")
    fun deactivateLeague(
        @PathVariable id: Long,
    ): ApiResponse<LeagueAdminResponse> {
        val league = leagueService.deactivate(id)
        return ApiResponse.success(LeagueAdminResponse.from(league))
    }

    /**
     * 리그를 활성화합니다.
     */
    @PostMapping("/{id}/activate")
    fun activateLeague(
        @PathVariable id: Long,
    ): ApiResponse<LeagueAdminResponse> {
        val league = leagueService.activate(id)
        return ApiResponse.success(LeagueAdminResponse.from(league))
    }
}
