package com.nextup.api.controller.oauth

import com.nextup.api.dto.oauth.LinkedOAuthAccountsResponse
import com.nextup.api.dto.oauth.OAuthLinkStartResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.infrastructure.service.oauth.OAuthLinkService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

/**
 * OAuth 계정 연동 관련 API Controller
 */
@RestController
@RequestMapping("/api/me/oauth-accounts")
class OAuthController(
    private val oauthLinkService: OAuthLinkService,
    @Value("\${app.oauth2.base-url:http://localhost:8080}")
    private val baseUrl: String,
) {
    /**
     * 연동된 소셜 계정 목록 조회
     *
     * GET /api/me/oauth-accounts
     */
    @GetMapping
    fun getLinkedOAuthAccounts(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<ApiResponse<LinkedOAuthAccountsResponse>> {
        val providers = oauthLinkService.getLinkedProviders(userId)
        val response = LinkedOAuthAccountsResponse(providers = providers)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 소셜 계정 연동 시작
     *
     * POST /api/me/oauth-accounts/{provider}/link
     *
     * OAuth2 인증 URL을 생성하여 반환합니다.
     * 클라이언트는 이 URL로 리다이렉트하여 OAuth2 플로우를 시작합니다.
     */
    @PostMapping("/{provider}/link")
    fun startOAuthLink(
        @PathVariable provider: OAuthProvider,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<ApiResponse<OAuthLinkStartResponse>> {
        // OAuth2 인증 URL 생성
        val authorizationUrl = buildAuthorizationUrl(provider)

        val response =
            OAuthLinkStartResponse(
                authorizationUrl = authorizationUrl,
                provider = provider,
            )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 소셜 계정 연결 해제
     *
     * DELETE /api/me/oauth-accounts/{provider}
     */
    @DeleteMapping("/{provider}")
    fun unlinkOAuthAccount(
        @PathVariable provider: OAuthProvider,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        oauthLinkService.unlinkOAuthAccount(userId, provider)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    /**
     * OAuth2 인증 URL을 생성합니다.
     *
     * Spring Security OAuth2 Client의 표준 엔드포인트를 사용합니다.
     * 형식: /oauth2/authorization/{registrationId}
     */
    private fun buildAuthorizationUrl(provider: OAuthProvider): String {
        val registrationId = provider.name.lowercase()

        return UriComponentsBuilder
            .fromUriString(baseUrl)
            .path("/oauth2/authorization/$registrationId")
            .build()
            .toUriString()
    }
}
