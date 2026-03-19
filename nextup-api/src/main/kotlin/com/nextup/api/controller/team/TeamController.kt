package com.nextup.api.controller.team

import com.nextup.api.dto.team.CreateTeamRequest
import com.nextup.api.dto.team.TeamDetailResponse
import com.nextup.api.dto.team.TeamSummaryResponse
import com.nextup.api.dto.team.UpdateTeamRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.LeagueNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.LeagueRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.team.TeamMembershipService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
 * 사용자 셀프서비스 팀 CRUD API
 *
 * 일반 사용자가 팀을 생성/수정/삭제/조회합니다.
 */
@RestController
@RequestMapping("/api/v1/teams")
class TeamController(
    private val teamMembershipService: TeamMembershipService,
    private val teamRepository: TeamRepositoryPort,
    private val leagueRepository: LeagueRepositoryPort,
) {
    /**
     * 팀을 생성합니다. 생성자가 OWNER로 자동 등록됩니다.
     *
     * POST /api/v1/teams
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeam(
        @RequestBody @Valid request: CreateTeamRequest,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<TeamDetailResponse> {
        val leagueId =
            request.leagueId
                ?: throw InvalidInputException("INVALID_INPUT", "리그 ID는 필수입니다")
        val league =
            leagueRepository.findByIdOrNull(leagueId)
                ?: throw LeagueNotFoundException(leagueId)

        val team =
            Team(
                league = league,
                name = request.name,
                city = request.city,
                abbreviation = request.abbreviation,
                foundedYear = LocalDate.now().year,
            )

        val savedTeam =
            teamMembershipService.createTeamWithOwner(
                userId = userId,
                team = team,
                uniformNumber = request.uniformNumber,
            )

        val memberCount = teamMembershipService.getTeamMemberCount(savedTeam.id)

        return ApiResponse.success(savedTeam.toDetailResponse(memberCount))
    }

    /**
     * 팀 정보를 수정합니다. OWNER만 가능합니다.
     *
     * PUT /api/v1/teams/{teamId}
     */
    @PutMapping("/{teamId}")
    @PreAuthorize("@teamSecurity.isOwner(#teamId, authentication.principal)")
    fun updateTeam(
        @PathVariable teamId: Long,
        @RequestBody @Valid request: UpdateTeamRequest,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<TeamDetailResponse> {
        val team =
            teamRepository.findByIdWithLeague(teamId)
                ?: throw TeamNotFoundException(teamId)

        team.updateBasicInfo(
            name = request.name,
            city = request.city,
            abbreviation = request.abbreviation,
        )

        val savedTeam = teamRepository.save(team)
        val memberCount = teamMembershipService.getTeamMemberCount(teamId)

        return ApiResponse.success(savedTeam.toDetailResponse(memberCount))
    }

    /**
     * 팀 상세 정보를 조회합니다.
     *
     * GET /api/v1/teams/{teamId}
     */
    @GetMapping("/{teamId}")
    fun getTeam(
        @PathVariable teamId: Long,
    ): ApiResponse<TeamDetailResponse> {
        val team =
            teamRepository.findByIdWithLeague(teamId)
                ?: throw TeamNotFoundException(teamId)
        val memberCount = teamMembershipService.getTeamMemberCount(teamId)

        return ApiResponse.success(team.toDetailResponse(memberCount))
    }

    /**
     * 팀 목록을 조회합니다. 이름, 도시로 필터링 가능합니다.
     *
     * GET /api/v1/teams?name=타이거즈&city=서울
     */
    @GetMapping
    fun getTeams(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) city: String?,
    ): ApiResponse<List<TeamSummaryResponse>> {
        val teams = teamRepository.findActiveTeamsByFilter(name, city)

        val teamIds = teams.map { it.id }
        val memberCounts = teamMembershipService.getTeamMemberCounts(teamIds)

        val responses =
            teams.map { team ->
                TeamSummaryResponse(
                    teamId = team.id,
                    name = team.name,
                    city = team.city,
                    abbreviation = team.abbreviation,
                    logoUrl = team.logoUrl,
                    memberCount = memberCounts[team.id] ?: 0,
                )
            }

        return ApiResponse.success(responses)
    }

    /**
     * 팀을 삭제합니다. OWNER만 가능하며, 멤버가 1명일 때만 삭제 가능합니다.
     *
     * DELETE /api/v1/teams/{teamId}
     */
    @DeleteMapping("/{teamId}")
    @PreAuthorize("@teamSecurity.isOwner(#teamId, authentication.principal)")
    fun deleteTeam(
        @PathVariable teamId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<Unit> {
        teamMembershipService.deleteTeam(teamId, userId)
        return ApiResponse.success(Unit)
    }

    private fun Team.toDetailResponse(memberCount: Int): TeamDetailResponse =
        TeamDetailResponse(
            teamId = this.id,
            name = this.name,
            city = this.city,
            abbreviation = this.abbreviation,
            logoUrl = this.logoUrl,
            leagueName = this.league.name,
            foundedYear = this.foundedYear,
            memberCount = memberCount,
        )
}
