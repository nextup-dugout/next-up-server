package com.nextup.api.dto.oauth

import com.nextup.core.domain.user.OAuthProvider
import jakarta.validation.constraints.NotNull

/**
 * OAuth 계정 연동 요청 DTO
 *
 * 소셜 로그인 연동 시작 시 사용됩니다.
 */
data class OAuthLinkRequest(
    @field:NotNull(message = "OAuth provider is required")
    val provider: OAuthProvider,
)
