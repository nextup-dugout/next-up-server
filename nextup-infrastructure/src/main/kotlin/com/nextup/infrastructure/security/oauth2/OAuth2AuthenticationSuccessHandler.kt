package com.nextup.infrastructure.security.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

/**
 * OAuth2 인증 성공 핸들러
 *
 * 인증 성공 시 일회용 인가 코드를 생성하고 프론트엔드로 리다이렉트합니다.
 * 프론트엔드는 인가 코드로 POST /api/auth/oauth2/token 요청하여 JWT 토큰을 수신합니다.
 *
 * 보안: accessToken/refreshToken을 URL query parameter에 직접 노출하지 않습니다.
 */
@Component
class OAuth2AuthenticationSuccessHandler(
    private val authCodeStore: AuthCodeStore,
    @Value("\${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private val redirectUri: String,
) : SimpleUrlAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication.principal as OAuth2UserPrincipal

        val authCode =
            authCodeStore.generate(
                userId = principal.userId,
                isNewUser = principal.isNewUser,
            )

        val targetUrl =
            UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("code", authCode)
                .build()
                .toUriString()

        response.sendRedirect(targetUrl)
    }
}
