package com.nextup.api.dto.auth

import com.nextup.infrastructure.security.TokenPair

/**
 * 토큰 응답 DTO
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
) {
    companion object {
        fun from(tokenPair: TokenPair): TokenResponse =
            TokenResponse(
                accessToken = tokenPair.accessToken,
                refreshToken = tokenPair.refreshToken,
            )
    }
}

/**
 * 현재 사용자 정보 응답 DTO
 */
data class CurrentUserResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val roles: Set<String>,
    val isActive: Boolean,
)

/**
 * 회원가입 응답 DTO
 */
data class SignUpResponse(
    val id: Long,
    val email: String,
    val nickname: String,
)
