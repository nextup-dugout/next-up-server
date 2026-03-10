package com.nextup.api.controller.user

import com.nextup.api.dto.user.*
import com.nextup.common.dto.ApiResponse
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
