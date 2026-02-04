package com.nextup.api.dto.user

import jakarta.validation.constraints.Size

/**
 * 프로필 수정 요청 DTO
 */
data class UpdateProfileRequest(
    @field:Size(min = 2, max = 50, message = "닉네임은 2~50자 사이여야 합니다")
    val nickname: String? = null,
    val profileImageUrl: String? = null,
)

/**
 * 비밀번호 변경 요청 DTO
 */
data class ChangePasswordRequest(
    val currentPassword: String,
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    val newPassword: String,
)
