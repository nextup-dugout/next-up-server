package com.nextup.scorer.config

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.socket.WebSocketSession

@DisplayName("WebSocketSessionRegistry")
class WebSocketSessionRegistryTest {
    private lateinit var registry: WebSocketSessionRegistry

    @BeforeEach
    fun setUp() {
        registry = WebSocketSessionRegistry()
    }

    @Nested
    @DisplayName("registerToken")
    inner class RegisterToken {
        @Test
        fun `should register token for session`() {
            // given
            val sessionId = "session-1"
            val token = "jwt-token-1"

            // when
            registry.registerToken(sessionId, token)

            // then
            val tokens = registry.getAllTokens()
            assertThat(tokens).containsEntry(sessionId, token)
            assertThat(registry.sessionCount()).isEqualTo(1)
        }

        @Test
        fun `should overwrite token when registering same session`() {
            // given
            val sessionId = "session-1"
            registry.registerToken(sessionId, "old-token")

            // when
            registry.registerToken(sessionId, "new-token")

            // then
            val tokens = registry.getAllTokens()
            assertThat(tokens[sessionId]).isEqualTo("new-token")
            assertThat(registry.sessionCount()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("registerSession")
    inner class RegisterSession {
        @Test
        fun `should register WebSocketSession reference`() {
            // given
            val session = mockk<WebSocketSession>()
            every { session.id } returns "session-1"

            // when
            registry.registerSession(session)

            // then
            assertThat(registry.getWebSocketSession("session-1")).isSameAs(session)
        }
    }

    @Nested
    @DisplayName("remove")
    inner class Remove {
        @Test
        fun `should remove both token and session reference`() {
            // given
            val sessionId = "session-1"
            registry.registerToken(sessionId, "jwt-token")
            val session = mockk<WebSocketSession>()
            every { session.id } returns sessionId
            registry.registerSession(session)

            // when
            registry.remove(sessionId)

            // then
            assertThat(registry.getAllTokens()).doesNotContainKey(sessionId)
            assertThat(registry.getWebSocketSession(sessionId)).isNull()
            assertThat(registry.sessionCount()).isEqualTo(0)
        }

        @Test
        fun `should do nothing when removing non-existent session`() {
            // when
            registry.remove("non-existent")

            // then
            assertThat(registry.sessionCount()).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("updateToken")
    inner class UpdateToken {
        @Test
        fun `should update token and return true when session exists`() {
            // given
            val sessionId = "session-1"
            registry.registerToken(sessionId, "old-token")

            // when
            val result = registry.updateToken(sessionId, "new-token")

            // then
            assertThat(result).isTrue()
            assertThat(registry.getAllTokens()[sessionId]).isEqualTo("new-token")
        }

        @Test
        fun `should return false when session does not exist`() {
            // when
            val result = registry.updateToken("non-existent", "new-token")

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("getAllTokens")
    inner class GetAllTokens {
        @Test
        fun `should return immutable snapshot of all tokens`() {
            // given
            registry.registerToken("session-1", "token-1")
            registry.registerToken("session-2", "token-2")

            // when
            val tokens = registry.getAllTokens()

            // then
            assertThat(tokens).hasSize(2)
            assertThat(tokens).containsEntry("session-1", "token-1")
            assertThat(tokens).containsEntry("session-2", "token-2")
        }

        @Test
        fun `should return empty map when no sessions registered`() {
            // when
            val tokens = registry.getAllTokens()

            // then
            assertThat(tokens).isEmpty()
        }
    }

    @Nested
    @DisplayName("sessionCount")
    inner class SessionCount {
        @Test
        fun `should return correct count`() {
            // given
            registry.registerToken("session-1", "token-1")
            registry.registerToken("session-2", "token-2")
            registry.registerToken("session-3", "token-3")

            // when & then
            assertThat(registry.sessionCount()).isEqualTo(3)
        }
    }
}
