package com.nextup.infrastructure.security.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * OAuth2 인증 실패 핸들러
 *
 * 인증 실패 시 에러 정보와 함께 프론트엔드로 리다이렉트합니다.
 */
@Component
class OAuth2AuthenticationFailureHandler(
    @Value("\${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private val redirectUri: String,
) : SimpleUrlAuthenticationFailureHandler() {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val errorMessage = exception.message ?: "Authentication failed"
        val encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)

        val targetUrl =
            UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("error", "oauth_error")
                .queryParam("message", encodedMessage)
                .build()
                .toUriString()

        response.sendRedirect(targetUrl)
    }
}
