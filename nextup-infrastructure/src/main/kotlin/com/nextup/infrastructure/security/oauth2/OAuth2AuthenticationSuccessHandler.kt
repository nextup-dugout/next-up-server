package com.nextup.infrastructure.security.oauth2

import com.nextup.core.domain.auth.RefreshToken
import com.nextup.infrastructure.repository.auth.RefreshTokenRepository
import com.nextup.infrastructure.security.jwt.JwtTokenProvider
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
 * 인증 성공 시 JWT 토큰을 발급하고 프론트엔드로 리다이렉트합니다.
 */
@Component
class OAuth2AuthenticationSuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private val redirectUri: String,
) : SimpleUrlAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication.principal as OAuth2UserPrincipal

        val roles = principal.getRoleNames()
        val accessToken =
            jwtTokenProvider.createAccessToken(
                userId = principal.userId,
                email = principal.email,
                roles = roles,
            )

        val refreshTokenString = jwtTokenProvider.createRefreshToken(principal.userId)
        val refreshToken =
            RefreshToken.create(
                userId = principal.userId,
                token = refreshTokenString,
                expiresAt = jwtTokenProvider.getRefreshTokenExpiration(),
                deviceInfo = request.getHeader("User-Agent"),
                ipAddress = getClientIpAddress(request),
            )
        refreshTokenRepository.save(refreshToken)

        val targetUrl =
            UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshTokenString)
                .queryParam("isNewUser", principal.isNewUser)
                .build()
                .toUriString()

        response.sendRedirect(targetUrl)
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }
}
