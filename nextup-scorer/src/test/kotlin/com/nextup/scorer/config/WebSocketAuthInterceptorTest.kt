package com.nextup.scorer.config

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private lateinit var sessionRegistry: WebSocketSessionRegistry
    private lateinit var interceptor: WebSocketAuthInterceptor

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = mockk()
        sessionRegistry = mockk(relaxed = true)
        interceptor = WebSocketAuthInterceptor(jwtTokenProvider, sessionRegistry)
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
        fun `should store token in credentials for later validation`() {
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
            interceptor.preSend(message, mockk())

            // then
            val auth = accessor.user as UsernamePasswordAuthenticationToken
            assertThat(auth.credentials).isEqualTo(token)
        }

        @Test
        fun `should register token in session registry on CONNECT`() {
            // given
            val token = "valid-jwt-token"
            val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
            accessor.setLeaveMutable(true)
            accessor.sessionId = "test-session-id"
            accessor.sessionAttributes = mutableMapOf()
            accessor.addNativeHeader("Authorization", "Bearer $token")
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns true
            every { jwtTokenProvider.getUserId(token) } returns 1L
            every { jwtTokenProvider.getRoles(token) } returns setOf("SCORER")

            // when
            interceptor.preSend(message, mockk())

            // then
            verify { sessionRegistry.registerToken("test-session-id", token) }
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
    @DisplayName("JWT expiry check on SEND/SUBSCRIBE")
    inner class JwtExpiryCheck {
        @Test
        fun `should reject SEND when token is expired`() {
            // given
            val token = "expired-token"
            val accessor = StompHeaderAccessor.create(StompCommand.SEND)
            accessor.destination = "/app/game/1/event"
            accessor.sessionAttributes = mutableMapOf<String, Any>("token" to token)
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(token) } returns false

            // when & then
            assertThatThrownBy {
                interceptor.preSend(message, mockk())
            }.isInstanceOf(AuthenticationCredentialsNotFoundException::class.java)
                .hasMessageContaining("JWT 토큰이 만료되었습니다")
        }

        @Test
        fun `should reject SUBSCRIBE when token is expired`() {
            // given
            val token = "expired-token"
            val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
            accessor.destination = "/topic/games/1/scoreboard"
            accessor.sessionAttributes = mutableMapOf<String, Any>("token" to token)
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(token) } returns false

            // when & then
            assertThatThrownBy {
                interceptor.preSend(message, mockk())
            }.isInstanceOf(AuthenticationCredentialsNotFoundException::class.java)
                .hasMessageContaining("JWT 토큰이 만료되었습니다")
        }

        @Test
        fun `should remove session from registry when token is expired`() {
            // given
            val token = "expired-token"
            val sessionId = "test-session-id"
            val accessor = StompHeaderAccessor.create(StompCommand.SEND)
            accessor.destination = "/app/game/1/event"
            accessor.sessionId = sessionId
            accessor.sessionAttributes = mutableMapOf<String, Any>("token" to token)
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(token) } returns false

            // when & then
            assertThatThrownBy {
                interceptor.preSend(message, mockk())
            }.isInstanceOf(AuthenticationCredentialsNotFoundException::class.java)

            verify { sessionRegistry.remove(sessionId) }
        }

        @Test
        fun `should allow SEND when token is still valid`() {
            // given
            val token = "valid-token"
            val accessor = StompHeaderAccessor.create(StompCommand.SEND)
            accessor.destination = "/app/game/1/event"
            accessor.sessionAttributes = mutableMapOf<String, Any>("token" to token)
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(token) } returns true

            // when
            val result = interceptor.preSend(message, mockk())

            // then
            assertThat(result).isNotNull
        }

        @Test
        fun `should pass through SEND without token in session`() {
            // given
            val accessor = StompHeaderAccessor.create(StompCommand.SEND)
            accessor.destination = "/app/game/1/event"
            accessor.sessionAttributes = mutableMapOf<String, Any>()
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            // when
            val result = interceptor.preSend(message, mockk())

            // then
            assertThat(result).isNotNull
        }
    }

    @Nested
    @DisplayName("Token refresh on SEND/SUBSCRIBE")
    inner class TokenRefresh {
        @Test
        fun `should refresh token when valid new token is provided in Authorization header`() {
            // given
            val oldToken = "old-valid-token"
            val newToken = "new-valid-token"
            val accessor = StompHeaderAccessor.create(StompCommand.SEND)
            accessor.destination = "/app/game/1/event"
            accessor.sessionAttributes = mutableMapOf<String, Any>("token" to oldToken)
            accessor.addNativeHeader("Authorization", "Bearer $newToken")
            accessor.setLeaveMutable(true)
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(newToken) } returns true
            every { jwtTokenProvider.isAccessToken(newToken) } returns true
            every { jwtTokenProvider.getUserId(newToken) } returns 1L
            every { jwtTokenProvider.getRoles(newToken) } returns setOf("SCORER")

            // when
            val result = interceptor.preSend(message, mockk())

            // then
            assertThat(result).isNotNull
            assertThat(accessor.sessionAttributes?.get("token")).isEqualTo(newToken)
            val auth = accessor.user as UsernamePasswordAuthenticationToken
            assertThat(auth.principal).isEqualTo(1L)
        }

        @Test
        fun `should update registry when token is refreshed`() {
            // given
            val oldToken = "old-valid-token"
            val newToken = "new-valid-token"
            val accessor = StompHeaderAccessor.create(StompCommand.SEND)
            accessor.destination = "/app/game/1/event"
            accessor.sessionId = "test-session-id"
            accessor.sessionAttributes = mutableMapOf<String, Any>("token" to oldToken)
            accessor.addNativeHeader("Authorization", "Bearer $newToken")
            accessor.setLeaveMutable(true)
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(newToken) } returns true
            every { jwtTokenProvider.isAccessToken(newToken) } returns true
            every { jwtTokenProvider.getUserId(newToken) } returns 1L
            every { jwtTokenProvider.getRoles(newToken) } returns setOf("SCORER")

            // when
            interceptor.preSend(message, mockk())

            // then
            verify { sessionRegistry.updateToken("test-session-id", newToken) }
        }

        @Test
        fun `should fall back to existing token validation when new token is invalid`() {
            // given
            val oldToken = "old-valid-token"
            val invalidNewToken = "invalid-new-token"
            val accessor = StompHeaderAccessor.create(StompCommand.SEND)
            accessor.destination = "/app/game/1/event"
            accessor.sessionAttributes = mutableMapOf<String, Any>("token" to oldToken)
            accessor.addNativeHeader("Authorization", "Bearer $invalidNewToken")
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(invalidNewToken) } returns false
            every { jwtTokenProvider.validateToken(oldToken) } returns true

            // when
            val result = interceptor.preSend(message, mockk())

            // then
            assertThat(result).isNotNull
        }

        @Test
        fun `should reject when both new and old tokens are expired`() {
            // given
            val oldToken = "expired-old-token"
            val expiredNewToken = "expired-new-token"
            val accessor = StompHeaderAccessor.create(StompCommand.SEND)
            accessor.destination = "/app/game/1/event"
            accessor.sessionAttributes = mutableMapOf<String, Any>("token" to oldToken)
            accessor.addNativeHeader("Authorization", "Bearer $expiredNewToken")
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

            every { jwtTokenProvider.validateToken(expiredNewToken) } returns false
            every { jwtTokenProvider.validateToken(oldToken) } returns false

            // when & then
            assertThatThrownBy {
                interceptor.preSend(message, mockk())
            }.isInstanceOf(AuthenticationCredentialsNotFoundException::class.java)
                .hasMessageContaining("JWT 토큰이 만료되었습니다")
        }
    }

    @Nested
    @DisplayName("Non-CONNECT commands")
    inner class NonConnect {
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
