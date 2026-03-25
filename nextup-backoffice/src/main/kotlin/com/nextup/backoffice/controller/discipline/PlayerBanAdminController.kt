package com.nextup.backoffice.controller.discipline

import com.nextup.backoffice.dto.discipline.IssuePlayerBanRequest
import com.nextup.backoffice.dto.discipline.PlayerBanAdminResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.discipline.PlayerBanService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * 선수 제재 관리 API Controller (관리자용)
 *
 * 전체 권한: 생성, 조회, 삭제
 */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice/player-bans")
class PlayerBanAdminController(
    private val playerBanService: PlayerBanService,
) {
    /**
     * 모든 제재 목록을 조회합니다.
     */
    @GetMapping
    fun getAllBans(
        @RequestParam(required = false) playerId: Long?,
        @RequestParam(required = false) competitionId: Long?,
    ): ApiResponse<List<PlayerBanAdminResponse>> {
        val bans =
            when {
                playerId != null && competitionId != null ->
                    playerBanService.getBansByPlayerAndCompetition(playerId, competitionId)

                playerId != null -> playerBanService.getBansByPlayer(playerId)
                competitionId != null -> playerBanService.getBansByCompetition(competitionId)
                else -> playerBanService.getAll()
            }

        return ApiResponse.success(
            bans.map { PlayerBanAdminResponse.from(it) },
        )
    }

    /**
     * 제재 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getBan(
        @PathVariable id: Long,
    ): ApiResponse<PlayerBanAdminResponse> {
        val ban = playerBanService.getById(id)
        return ApiResponse.success(PlayerBanAdminResponse.from(ban))
    }

    /**
     * 선수가 출장 가능한지 확인합니다.
     */
    @GetMapping("/eligibility")
    fun checkPlayerEligibility(
        @RequestParam playerId: Long,
        @RequestParam competitionId: Long,
    ): ApiResponse<Map<String, Boolean>> {
        val canPlay = playerBanService.canPlayerPlay(playerId, competitionId)
        return ApiResponse.success(mapOf("canPlay" to canPlay))
    }

    /**
     * 선수 제재를 발급합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun issueBan(
        @Valid @RequestBody request: IssuePlayerBanRequest,
    ): ApiResponse<PlayerBanAdminResponse> {
        val ban =
            playerBanService.issueBan(
                playerId = request.playerId,
                competitionId = request.competitionId,
                reason = request.reason,
                issuedBy = request.issuedBy,
            )

        return ApiResponse.success(PlayerBanAdminResponse.from(ban))
    }

    /**
     * 선수 제재를 삭제합니다.
     */
    @DeleteMapping("/{id}")
    fun deleteBan(
        @PathVariable id: Long,
    ): ApiResponse<Unit> {
        playerBanService.deleteBan(id)
        return ApiResponse.success(Unit)
    }
}
