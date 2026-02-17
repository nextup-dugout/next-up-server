package com.nextup.scorer.config

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

@DisplayName("WebSocketAuthInterceptor")
class WebSocketAuthInterceptorTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var interceptor: WebSocketAuthInterceptor

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = mockk()
        interceptor = WebSocketAuthInterceptor(jwtTokenProvider)
    }

    @Nested
    @DisplayName("STOMP CONNECT")
    inner class StompConnect {
        @Test
        fun `should authenticate with valid token`() {
            // given
            val token = "valid-jwt-token"
            val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
            accessor.setLeaveMutable(true)
            accessor.addNativeHeader("Authorization", "Bearer $token")
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns true
            every { jwtTokenProvider.getUserId(token) } returns 1L
            every { jwtTokenProvider.getRoles(token) } returns setOf("SCORER")

            // when
            val result = interceptor.preSend(message, mockk())

            // then
            assertThat(result).isNotNull
            assertThat(accessor.user).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
            val auth = accessor.user as UsernamePasswordAuthenticationToken
            assertThat(auth.principal).isEqualTo(1L)
        }

        @Test
        fun `should reject when no Authorization header`() {
            // given
            val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            // when & then
            assertThatThrownBy {
                interceptor.preSend(message, mockk())
            }.isInstanceOf(AuthenticationCredentialsNotFoundException::class.java)
                .hasMessageContaining("인증 토큰이 필요합니다")
        }

        @Test
        fun `should reject when token is invalid`() {
            // given
            val token = "invalid-token"
            val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
            accessor.addNativeHeader("Authorization", "Bearer $token")
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(token) } returns false

            // when & then
            assertThatThrownBy {
                interceptor.preSend(message, mockk())
            }.isInstanceOf(AuthenticationCredentialsNotFoundException::class.java)
                .hasMessageContaining("유효하지 않은 토큰입니다")
        }

        @Test
        fun `should reject when token is not access token`() {
            // given
            val token = "refresh-token"
            val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
            accessor.addNativeHeader("Authorization", "Bearer $token")
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns false

            // when & then
            assertThatThrownBy {
                interceptor.preSend(message, mockk())
            }.isInstanceOf(AuthenticationCredentialsNotFoundException::class.java)
                .hasMessageContaining("유효하지 않은 토큰입니다")
        }
    }

    @Nested
    @DisplayName("Non-CONNECT commands")
    inner class NonConnect {
        @Test
        fun `should pass through SUBSCRIBE without auth check`() {
            // given
            val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
            accessor.destination = "/topic/games/1/scoreboard"
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            // when
            val result = interceptor.preSend(message, mockk())

            // then
            assertThat(result).isNotNull
        }

        @Test
        fun `should pass through DISCONNECT without auth check`() {
            // given
            val accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT)
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            // when
            val result = interceptor.preSend(message, mockk())

            // then
            assertThat(result).isNotNull
        }
    }
}
