package com.nextup.api.controller.player

import com.nextup.api.dto.player.MyPlayerProfileResponse
import com.nextup.api.dto.player.UpdatePlayerProfileRequest
import com.nextup.api.dto.player.toMyProfileResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.player.PlayerService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 프로필 API Controller
 *
 * 인증된 사용자가 본인의 선수 프로필을 조회/수정하는 API를 제공합니다.
 *
 * GET  /api/v1/players/me  - 내 선수 프로필 조회
 * PUT  /api/v1/players/me  - 내 선수 프로필 수정
 */
@RestController
@RequestMapping("/api/v1/players/me")
class PlayerProfileController(
    private val playerService: PlayerService,
) {
    /**
     * 내 선수 프로필을 조회합니다.
     *
     * @param userId 인증된 사용자 ID
     * @return 선수 프로필 정보
     */
    @GetMapping
    fun getMyPlayerProfile(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<MyPlayerProfileResponse> {
        val player = playerService.getLinkedPlayer(userId)
        return ApiResponse.success(player.toMyProfileResponse())
    }

    /**
     * 내 선수 프로필을 수정합니다.
     *
     * @param userId 인증된 사용자 ID
     * @param request 수정할 선수 프로필 정보
     * @return 수정된 선수 프로필 정보
     */
    @PutMapping
    fun updateMyPlayerProfile(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: UpdatePlayerProfileRequest,
    ): ApiResponse<MyPlayerProfileResponse> {
        val player =
            playerService.updatePlayerProfile(
                userId = userId,
                primaryPosition = request.primaryPosition,
                throwingHand = request.throwingHand,
                battingHand = request.battingHand,
                height = request.height,
                weight = request.weight,
            )
        return ApiResponse.success(player.toMyProfileResponse())
    }
}
