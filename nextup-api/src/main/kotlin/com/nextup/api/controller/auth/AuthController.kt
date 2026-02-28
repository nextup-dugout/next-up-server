package com.nextup.api.controller.auth

import com.nextup.api.dto.auth.*
import com.nextup.common.dto.ApiResponse
import com.nextup.infrastructure.security.AuthenticationService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인증 관련 API Controller
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationService: AuthenticationService,
) {
    /**
     * 로그인
     *
     * POST /api/auth/login
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<TokenResponse>> {
        val deviceInfo = httpRequest.getHeader("User-Agent")
        val ipAddress = getClientIpAddress(httpRequest)

        val tokenPair =
            authenticationService.login(
                email = request.email,
                password = request.password,
                deviceInfo = deviceInfo,
                ipAddress = ipAddress,
            )

        return ResponseEntity.ok(ApiResponse.success(TokenResponse.from(tokenPair)))
    }

    /**
     * 토큰 갱신
     *
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<TokenResponse>> {
        val deviceInfo = httpRequest.getHeader("User-Agent")
        val ipAddress = getClientIpAddress(httpRequest)

        val tokenPair =
            authenticationService.refresh(
                refreshTokenString = request.refreshToken,
                deviceInfo = deviceInfo,
                ipAddress = ipAddress,
            )

        return ResponseEntity.ok(ApiResponse.success(TokenResponse.from(tokenPair)))
    }

    /**
     * 로그아웃
     *
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: LogoutRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        authenticationService.logout(request.refreshToken)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    /**
     * 전체 로그아웃 (모든 디바이스)
     *
     * POST /api/auth/logout-all
     */
    @PostMapping("/logout-all")
    fun logoutAll(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        authenticationService.logoutAll(userId)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    /**
     * OAuth2 인가 코드로 JWT 토큰 교환
     *
     * POST /api/auth/oauth2/token
     */
    @PostMapping("/oauth2/token")
    fun exchangeOAuth2Token(
        @Valid @RequestBody request: OAuth2TokenExchangeRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<OAuth2TokenResponse>> {
        val deviceInfo = httpRequest.getHeader("User-Agent")
        val ipAddress = getClientIpAddress(httpRequest)

        val result =
            authenticationService.exchangeOAuth2Code(
                code = request.code,
                deviceInfo = deviceInfo,
                ipAddress = ipAddress,
            )

        return ResponseEntity.ok(
            ApiResponse.success(
                OAuth2TokenResponse(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    isNewUser = result.isNewUser,
                ),
            ),
        )
    }

    /**
     * 현재 사용자 정보 조회
     *
     * GET /api/auth/me
     */
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<ApiResponse<CurrentUserResponse>> {
        val userDetails = authenticationService.getUserDetails(userId)

        val response =
            CurrentUserResponse(
                id = userDetails.id,
                email = userDetails.email,
                nickname = userDetails.nickname,
                roles = userDetails.getRoleNames(),
                isActive = userDetails.isEnabled,
            )

        return ResponseEntity.ok(ApiResponse.success(response))
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
