package com.nextup.scorer.config

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

/**
 * WebSocket HTTP 핸드셰이크 단계에서 JWT 토큰을 검증하는 인터셉터.
 *
 * Spring Security의 permitAll()로 WebSocket 경로를 열어두더라도,
 * 이 인터셉터가 핸드셰이크 시점에 JWT를 검증하여 무인증 연결을 차단합니다.
 *
 * 클라이언트는 다음 두 가지 방식으로 토큰을 전달할 수 있습니다:
 * 1. 쿼리 파라미터: ws/scoreboard?token={JWT}
 * 2. Authorization 헤더: Bearer {JWT}
 *
 * STOMP CONNECT 단계의 WebSocketAuthInterceptor와 함께 이중 검증(defense-in-depth)을 제공합니다.
 */
@Component
class WebSocketHandshakeInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
) : HandshakeInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        internal const val TOKEN_PARAM = "token"
    }

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val token = extractToken(request)

        if (token == null) {
            log.warn("WebSocket 핸드셰이크 거부: 인증 토큰 없음")
            return false
        }

        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
            log.warn("WebSocket 핸드셰이크 거부: 유효하지 않은 토큰")
            return false
        }

        val userId = jwtTokenProvider.getUserId(token)
        attributes["userId"] = userId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        // no-op
    }

    private fun extractToken(request: ServerHttpRequest): String? {
        // 1. 쿼리 파라미터에서 추출
        if (request is ServletServerHttpRequest) {
            val token = request.servletRequest.getParameter(TOKEN_PARAM)
            if (!token.isNullOrBlank()) {
                return token
            }
        }

        // 2. Authorization 헤더에서 추출
        val authHeader = request.headers.getFirst("Authorization")
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.removePrefix(BEARER_PREFIX)
        }

        return null
    }
}
