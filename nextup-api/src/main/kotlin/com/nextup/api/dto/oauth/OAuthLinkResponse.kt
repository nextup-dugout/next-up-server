package com.nextup.api.dto.oauth

import com.nextup.core.domain.user.OAuthProvider

/**
 * OAuth 계정 연동 응답 DTO
 */
data class OAuthLinkResponse(
    val provider: OAuthProvider,
    val linkedAt: String,
)

/**
 * 연동된 OAuth 계정 목록 응답 DTO
 */
data class LinkedOAuthAccountsResponse(
    val providers: List<OAuthProvider>,
)

/**
 * OAuth 계정 연동 시작 응답 DTO
 */
data class OAuthLinkStartResponse(
    val authorizationUrl: String,
    val provider: OAuthProvider,
)
