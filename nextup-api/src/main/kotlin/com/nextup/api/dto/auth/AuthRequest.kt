package com.nextup.api.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 로그인 요청 DTO
 */
data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
)

/**
 * 회원가입 요청 DTO
 */
data class SignUpRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    val password: String,
    @field:NotBlank(message = "Nickname is required")
    @field:Size(min = 2, max = 50, message = "Nickname must be 2-50 characters")
    val nickname: String,
)

/**
 * 토큰 갱신 요청 DTO
 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,
)

/**
 * 로그아웃 요청 DTO
 */
data class LogoutRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,
)

/**
 * 비밀번호 변경 요청 DTO
 */
data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    val newPassword: String,
)
