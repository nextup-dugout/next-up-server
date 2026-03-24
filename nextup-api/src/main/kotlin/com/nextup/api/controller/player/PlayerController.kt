package com.nextup.api.controller.player

import com.nextup.api.dto.player.CreateUnaffiliatedPlayerApiRequest
import com.nextup.api.dto.player.PlayerDetailResponse
import com.nextup.api.dto.player.PlayerSearchResponse
import com.nextup.api.dto.player.toDetailResponse
import com.nextup.api.dto.player.toSearchResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.common.PageResult
import com.nextup.core.domain.player.Position
import com.nextup.core.service.player.PlayerService
import com.nextup.core.service.player.PlayerTeamService
import com.nextup.infrastructure.common.toPageCommand
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 검색/조회 API Controller
 *
 * GET /api/v1/players         - 선수 검색/목록 (name, teamId, position 필터, Pageable)
 * GET /api/v1/players/{id}    - 선수 단건 상세 조회
 */
@RestController
@RequestMapping("/api/v1/players")
class PlayerController(
    private val playerService: PlayerService,
    private val playerTeamService: PlayerTeamService,
) {
    /**
     * 무소속 선수 프로필을 생성합니다.
     *
     * POST /api/v1/players/unaffiliated
     *
     * 팀에 소속되지 않은 사용자가 이벤트 게임 등에 참가하기 위해
     * 선수 프로필을 생성합니다.
     *
     * @param userId 인증된 사용자 ID
     * @param request 선수 프로필 생성 요청
     * @return 생성된 선수 상세 정보
     */
    @PostMapping("/unaffiliated")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    fun createUnaffiliatedPlayer(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateUnaffiliatedPlayerApiRequest,
    ): ApiResponse<PlayerDetailResponse> {
        val player =
            playerService.createUnaffiliatedPlayer(
                userId = userId,
                name = request.name,
                primaryPosition = request.primaryPosition,
                throwingHand = request.throwingHand,
                battingHand = request.battingHand,
            )
        return ApiResponse.success(player.toDetailResponse(currentHistory = null))
    }

    /**
     * 선수 검색/목록 조회
     *
     * GET /api/v1/players?name={name}&teamId={teamId}&position={position}
     *
     * @param name 이름 부분 검색 (선택)
     * @param teamId 팀 ID 필터 (선택)
     * @param position 포지션 필터 (선택, Position enum 이름)
     * @param pageable 페이징 정보 (기본: size=20, sort=name,asc)
     * @return 선수 검색 결과 페이지
     */
    @GetMapping
    fun searchPlayers(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) teamId: Long?,
        @RequestParam(required = false) position: Position?,
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC)
        pageable: Pageable,
    ): ApiResponse<PageResult<PlayerSearchResponse>> {
        val players =
            playerService.search(
                name = name,
                teamId = teamId,
                position = position,
                pageCommand = pageable.toPageCommand(),
            )
        val result =
            players.map { player ->
                val currentHistory =
                    playerTeamService.getActiveAffiliationsByPlayer(player.id).firstOrNull()
                player.toSearchResponse(currentHistory)
            }
        return ApiResponse.success(result)
    }

    /**
     * 선수 단건 상세 조회
     *
     * GET /api/v1/players/{playerId}
     *
     * @param playerId 선수 ID
     * @return 선수 상세 정보
     */
    @GetMapping("/{playerId}")
    fun getPlayer(
        @PathVariable playerId: Long,
    ): ApiResponse<PlayerDetailResponse> {
        val player = playerService.getById(playerId)
        val currentHistory =
            playerTeamService.getActiveAffiliationsByPlayer(playerId).firstOrNull()
        return ApiResponse.success(player.toDetailResponse(currentHistory))
    }
}
