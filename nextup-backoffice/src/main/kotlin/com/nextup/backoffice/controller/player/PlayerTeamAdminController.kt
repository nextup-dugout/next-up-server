package com.nextup.backoffice.controller.player

import com.nextup.backoffice.dto.player.*
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.player.PlayerTeamService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * 선수-팀 소속 관리 API Controller (관리자용)
 */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice/player-teams")
class PlayerTeamAdminController(
    private val playerTeamService: PlayerTeamService,
) {
    /**
     * 선수를 팀에 소속시킵니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registerAffiliation(
        @Valid @RequestBody request: RegisterAffiliationRequest,
    ): ApiResponse<PlayerTeamResponse> {
        val affiliation =
            playerTeamService.registerAffiliation(
                playerId = request.playerId,
                teamId = request.teamId,
                startDate = request.startDate,
                position = request.position,
                uniformNumber = request.uniformNumber,
            )
        return ApiResponse.success(PlayerTeamResponse.from(affiliation))
    }

    /**
     * 선수의 소속을 종료합니다 (탈퇴 처리).
     */
    @PutMapping("/{id}/end")
    fun endAffiliation(
        @PathVariable id: Long,
        @Valid @RequestBody request: EndAffiliationRequest,
    ): ApiResponse<PlayerTeamResponse> {
        val affiliation =
            playerTeamService.endAffiliation(
                affiliationId = id,
                endDate = request.endDate,
            )
        return ApiResponse.success(PlayerTeamResponse.from(affiliation))
    }

    /**
     * 등번호를 변경합니다.
     */
    @PutMapping("/{id}/uniform-number")
    fun changeUniformNumber(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangeUniformNumberRequest,
    ): ApiResponse<PlayerTeamResponse> {
        val affiliation =
            playerTeamService.changeUniformNumber(
                affiliationId = id,
                uniformNumber = request.uniformNumber,
            )
        return ApiResponse.success(PlayerTeamResponse.from(affiliation))
    }

    /**
     * 포지션을 변경합니다.
     */
    @PutMapping("/{id}/position")
    fun changePosition(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangePositionRequest,
    ): ApiResponse<PlayerTeamResponse> {
        val affiliation =
            playerTeamService.changePosition(
                affiliationId = id,
                position = request.position,
            )
        return ApiResponse.success(PlayerTeamResponse.from(affiliation))
    }

    /**
     * 선수의 활성 소속 목록을 조회합니다.
     */
    @GetMapping("/player/{playerId}")
    fun getPlayerAffiliations(
        @PathVariable playerId: Long,
    ): ApiResponse<List<PlayerTeamResponse>> {
        val affiliations = playerTeamService.getActiveAffiliationsByPlayer(playerId)
        return ApiResponse.success(
            affiliations.map { PlayerTeamResponse.from(it) },
        )
    }

    /**
     * 팀의 현재 로스터를 조회합니다.
     */
    @GetMapping("/team/{teamId}/roster")
    fun getTeamRoster(
        @PathVariable teamId: Long,
    ): ApiResponse<List<PlayerTeamResponse>> {
        val roster = playerTeamService.getTeamRoster(teamId)
        return ApiResponse.success(
            roster.map { PlayerTeamResponse.from(it) },
        )
    }

    /**
     * 선수의 전체 소속 이력을 조회합니다.
     */
    @GetMapping("/player/{playerId}/history")
    fun getPlayerHistory(
        @PathVariable playerId: Long,
    ): ApiResponse<List<PlayerTeamResponse>> {
        val history = playerTeamService.getPlayerHistory(playerId)
        return ApiResponse.success(
            history.map { PlayerTeamResponse.from(it) },
        )
    }
}
