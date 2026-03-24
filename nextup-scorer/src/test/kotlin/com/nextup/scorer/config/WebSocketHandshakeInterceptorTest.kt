package com.nextup.scorer.config

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.web.socket.WebSocketHandler

@DisplayName("WebSocketHandshakeInterceptor")
class WebSocketHandshakeInterceptorTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var interceptor: WebSocketHandshakeInterceptor

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = mockk()
        interceptor = WebSocketHandshakeInterceptor(jwtTokenProvider)
    }

    @Nested
    @DisplayName("beforeHandshake - Authorization 헤더")
    inner class AuthorizationHeader {
        @Test
        fun `should accept handshake with valid Bearer token in Authorization header`() {
            // given
            val token = "valid-jwt-token"
            val headers = HttpHeaders()
            headers.set("Authorization", "Bearer $token")
            val request = mockk<ServerHttpRequest>()
            every { request.headers } returns headers
            val response = mockk<ServerHttpResponse>()
            val wsHandler = mockk<WebSocketHandler>()
            val attributes = mutableMapOf<String, Any>()

            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns true
            every { jwtTokenProvider.getUserId(token) } returns 42L

            // when
            val result =
                interceptor.beforeHandshake(request, response, wsHandler, attributes)

            // then
            assertThat(result).isTrue()
            assertThat(attributes["userId"]).isEqualTo(42L)
        }

        @Test
        fun `should reject handshake with invalid token in Authorization header`() {
            // given
            val token = "invalid-token"
            val headers = HttpHeaders()
            headers.set("Authorization", "Bearer $token")
            val request = mockk<ServerHttpRequest>()
            every { request.headers } returns headers
            val response = mockk<ServerHttpResponse>()
            val wsHandler = mockk<WebSocketHandler>()
            val attributes = mutableMapOf<String, Any>()

            every { jwtTokenProvider.validateToken(token) } returns false

            // when
            val result =
                interceptor.beforeHandshake(request, response, wsHandler, attributes)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `should reject handshake when token is not access token`() {
            // given
            val token = "refresh-token"
            val headers = HttpHeaders()
            headers.set("Authorization", "Bearer $token")
            val request = mockk<ServerHttpRequest>()
            every { request.headers } returns headers
            val response = mockk<ServerHttpResponse>()
            val wsHandler = mockk<WebSocketHandler>()
            val attributes = mutableMapOf<String, Any>()

            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns false

            // when
            val result =
                interceptor.beforeHandshake(request, response, wsHandler, attributes)

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("beforeHandshake - 쿼리 파라미터")
    inner class QueryParameter {
        @Test
        fun `should accept handshake with valid token in query parameter`() {
            // given
            val token = "valid-jwt-token"
            val servletRequest = mockk<jakarta.servlet.http.HttpServletRequest>()
            every { servletRequest.getParameter("token") } returns token
            val request = mockk<ServletServerHttpRequest>()
            every { request.servletRequest } returns servletRequest
            every { request.headers } returns HttpHeaders()
            val response = mockk<ServerHttpResponse>()
            val wsHandler = mockk<WebSocketHandler>()
            val attributes = mutableMapOf<String, Any>()

            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns true
            every { jwtTokenProvider.getUserId(token) } returns 7L

            // when
            val result =
                interceptor.beforeHandshake(request, response, wsHandler, attributes)

            // then
            assertThat(result).isTrue()
            assertThat(attributes["userId"]).isEqualTo(7L)
        }

        @Test
        fun `should reject handshake with invalid token in query parameter`() {
            // given
            val token = "invalid-token"
            val servletRequest = mockk<jakarta.servlet.http.HttpServletRequest>()
            every { servletRequest.getParameter("token") } returns token
            val request = mockk<ServletServerHttpRequest>()
            every { request.servletRequest } returns servletRequest
            every { request.headers } returns HttpHeaders()
            val response = mockk<ServerHttpResponse>()
            val wsHandler = mockk<WebSocketHandler>()
            val attributes = mutableMapOf<String, Any>()

            every { jwtTokenProvider.validateToken(token) } returns false

            // when
            val result =
                interceptor.beforeHandshake(request, response, wsHandler, attributes)

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("beforeHandshake - 토큰 없음")
    inner class NoToken {
        @Test
        fun `should reject handshake when no token provided`() {
            // given
            val request = mockk<ServerHttpRequest>()
            every { request.headers } returns HttpHeaders()
            val response = mockk<ServerHttpResponse>()
            val wsHandler = mockk<WebSocketHandler>()
            val attributes = mutableMapOf<String, Any>()

            // when
            val result =
                interceptor.beforeHandshake(request, response, wsHandler, attributes)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `should reject handshake when query param is blank`() {
            // given
            val servletRequest = mockk<jakarta.servlet.http.HttpServletRequest>()
            every { servletRequest.getParameter("token") } returns "  "
            val request = mockk<ServletServerHttpRequest>()
            every { request.servletRequest } returns servletRequest
            every { request.headers } returns HttpHeaders()
            val response = mockk<ServerHttpResponse>()
            val wsHandler = mockk<WebSocketHandler>()
            val attributes = mutableMapOf<String, Any>()

            // when
            val result =
                interceptor.beforeHandshake(request, response, wsHandler, attributes)

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("beforeHandshake - 토큰 우선순위")
    inner class TokenPriority {
        @Test
        fun `should prefer query parameter token over Authorization header`() {
            // given
            val queryToken = "query-token"
            val headerToken = "header-token"

            val servletRequest = mockk<jakarta.servlet.http.HttpServletRequest>()
            every { servletRequest.getParameter("token") } returns queryToken
            val headers = HttpHeaders()
            headers.set("Authorization", "Bearer $headerToken")
            val request = mockk<ServletServerHttpRequest>()
            every { request.servletRequest } returns servletRequest
            every { request.headers } returns headers
            val response = mockk<ServerHttpResponse>()
            val wsHandler = mockk<WebSocketHandler>()
            val attributes = mutableMapOf<String, Any>()

            every { jwtTokenProvider.validateToken(queryToken) } returns true
            every { jwtTokenProvider.isAccessToken(queryToken) } returns true
            every { jwtTokenProvider.getUserId(queryToken) } returns 99L

            // when
            val result =
                interceptor.beforeHandshake(request, response, wsHandler, attributes)

            // then
            assertThat(result).isTrue()
            assertThat(attributes["userId"]).isEqualTo(99L)
        }
    }
}
