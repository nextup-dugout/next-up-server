package com.nextup.api.controller.user

import com.nextup.api.dto.game.GameSummaryResponse
import com.nextup.api.dto.user.*
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.GameScheduleService
import com.nextup.core.service.team.TeamMembershipService
import com.nextup.core.service.user.UserService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 사용자 프로필 API Controller
 *
 * 로그인한 사용자가 본인 정보를 조회/수정하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/me")
class ProfileController(
    private val userService: UserService,
    private val teamMembershipService: TeamMembershipService,
    private val gameScheduleService: GameScheduleService,
) {
    /**
     * 내 프로필을 조회합니다.
     */
    @GetMapping
    fun getMyProfile(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<ProfileResponse> {
        val user = userService.getActiveById(userId)
        return ApiResponse.success(ProfileResponse.from(user))
    }

    /**
     * 내 소속 팀 목록을 조회합니다.
     */
    @GetMapping("/teams")
    fun getMyTeams(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<MyTeamResponse>> {
        val activeMembers = teamMembershipService.getActiveTeamsByUserId(userId)
        return ApiResponse.success(activeMembers.map { MyTeamResponse.from(it) })
    }

    /**
     * 내 소속 팀들의 다가오는 경기를 통합 조회합니다.
     */
    @GetMapping("/upcoming-games")
    fun getMyUpcomingGames(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "10") limit: Int,
    ): ApiResponse<List<GameSummaryResponse>> {
        val activeMembers = teamMembershipService.getActiveTeamsByUserId(userId)
        val teamIds = activeMembers.map { it.team.id }
        val games = gameScheduleService.getUpcomingGamesByTeamIds(teamIds, limit)
        return ApiResponse.success(games.map { GameSummaryResponse.from(it) })
    }

    /**
     * 내 프로필을 수정합니다.
     */
    @PutMapping
    fun updateMyProfile(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): ApiResponse<ProfileResponse> {
        val user =
            userService.updateProfile(
                userId = userId,
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
            )
        return ApiResponse.success(ProfileResponse.from(user))
    }

    /**
     * 회원 탈퇴합니다 (계정 비활성화).
     */
    @DeleteMapping
    fun deactivateMyAccount(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<Unit> {
        userService.deactivate(userId)
        return ApiResponse.success(Unit)
    }
}

/**
 * 공개 프로필 API Controller
 *
 * 다른 사용자의 공개 프로필을 조회하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/users")
class PublicProfileController(
    private val userService: UserService,
) {
    /**
     * 사용자의 공개 프로필을 조회합니다.
     */
    @GetMapping("/{userId}")
    fun getPublicProfile(
        @PathVariable userId: Long,
    ): ApiResponse<PublicProfileResponse> {
        val user = userService.getActiveById(userId)
        return ApiResponse.success(PublicProfileResponse.from(user))
    }
}
