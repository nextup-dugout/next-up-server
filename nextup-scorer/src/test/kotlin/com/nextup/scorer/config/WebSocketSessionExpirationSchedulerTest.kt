package com.nextup.scorer.config

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.socket.WebSocketSession

@DisplayName("WebSocketSessionExpirationScheduler")
class WebSocketSessionExpirationSchedulerTest {
    private lateinit var sessionRegistry: WebSocketSessionRegistry
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var scheduler: WebSocketSessionExpirationScheduler

    @BeforeEach
    fun setUp() {
        sessionRegistry = mockk(relaxed = true)
        jwtTokenProvider = mockk()
        scheduler =
            WebSocketSessionExpirationScheduler(
                sessionRegistry = sessionRegistry,
                jwtTokenProvider = jwtTokenProvider,
            )
    }

    @Nested
    @DisplayName("checkExpiredSessions")
    inner class CheckExpiredSessions {
        @Test
        fun `should do nothing when no sessions exist`() {
            // given
            every { sessionRegistry.getAllTokens() } returns emptyMap()

            // when
            scheduler.checkExpiredSessions()

            // then
            verify(exactly = 0) { sessionRegistry.remove(any()) }
        }

        @Test
        fun `should disconnect session with expired token`() {
            // given
            val sessionId = "expired-session"
            val expiredToken = "expired-token"
            val mockSession = mockk<WebSocketSession>(relaxed = true)

            every { sessionRegistry.getAllTokens() } returns mapOf(sessionId to expiredToken)
            every { jwtTokenProvider.validateToken(expiredToken) } returns false
            every { sessionRegistry.getWebSocketSession(sessionId) } returns mockSession
            every { mockSession.isOpen } returns true

            // when
            scheduler.checkExpiredSessions()

            // then
            verify { mockSession.close(WebSocketSessionExpirationScheduler.JWT_EXPIRED_CLOSE_STATUS) }
            verify { sessionRegistry.remove(sessionId) }
        }

        @Test
        fun `should not disconnect session with valid token`() {
            // given
            val sessionId = "valid-session"
            val validToken = "valid-token"

            every { sessionRegistry.getAllTokens() } returns mapOf(sessionId to validToken)
            every { jwtTokenProvider.validateToken(validToken) } returns true

            // when
            scheduler.checkExpiredSessions()

            // then
            verify(exactly = 0) { sessionRegistry.remove(any()) }
        }

        @Test
        fun `should handle mixed valid and expired sessions`() {
            // given
            val validSessionId = "valid-session"
            val expiredSessionId = "expired-session"
            val validToken = "valid-token"
            val expiredToken = "expired-token"
            val mockSession = mockk<WebSocketSession>(relaxed = true)

            every { sessionRegistry.getAllTokens() } returns
                mapOf(
                    validSessionId to validToken,
                    expiredSessionId to expiredToken,
                )
            every { jwtTokenProvider.validateToken(validToken) } returns true
            every { jwtTokenProvider.validateToken(expiredToken) } returns false
            every { sessionRegistry.getWebSocketSession(expiredSessionId) } returns mockSession
            every { mockSession.isOpen } returns true

            // when
            scheduler.checkExpiredSessions()

            // then
            verify(exactly = 1) { sessionRegistry.remove(expiredSessionId) }
            verify(exactly = 0) { sessionRegistry.remove(validSessionId) }
        }

        @Test
        fun `should handle already closed session gracefully`() {
            // given
            val sessionId = "closed-session"
            val expiredToken = "expired-token"
            val mockSession = mockk<WebSocketSession>(relaxed = true)

            every { sessionRegistry.getAllTokens() } returns mapOf(sessionId to expiredToken)
            every { jwtTokenProvider.validateToken(expiredToken) } returns false
            every { sessionRegistry.getWebSocketSession(sessionId) } returns mockSession
            every { mockSession.isOpen } returns false

            // when
            scheduler.checkExpiredSessions()

            // then
            verify(exactly = 0) { mockSession.close(any()) }
            verify { sessionRegistry.remove(sessionId) }
        }

        @Test
        fun `should handle null WebSocketSession gracefully`() {
            // given
            val sessionId = "null-session"
            val expiredToken = "expired-token"

            every { sessionRegistry.getAllTokens() } returns mapOf(sessionId to expiredToken)
            every { jwtTokenProvider.validateToken(expiredToken) } returns false
            every { sessionRegistry.getWebSocketSession(sessionId) } returns null

            // when
            scheduler.checkExpiredSessions()

            // then
            verify { sessionRegistry.remove(sessionId) }
        }

        @Test
        fun `should still remove session from registry when close throws exception`() {
            // given
            val sessionId = "error-session"
            val expiredToken = "expired-token"
            val mockSession = mockk<WebSocketSession>(relaxed = true)

            every { sessionRegistry.getAllTokens() } returns mapOf(sessionId to expiredToken)
            every { jwtTokenProvider.validateToken(expiredToken) } returns false
            every { sessionRegistry.getWebSocketSession(sessionId) } returns mockSession
            every { mockSession.isOpen } returns true
            every { mockSession.close(any()) } throws RuntimeException("Connection reset")

            // when
            scheduler.checkExpiredSessions()

            // then
            verify { sessionRegistry.remove(sessionId) }
        }
    }
}
