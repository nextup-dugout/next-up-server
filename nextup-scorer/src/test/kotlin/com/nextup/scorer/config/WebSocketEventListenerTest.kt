package com.nextup.scorer.config

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@DisplayName("WebSocketEventListener")
class WebSocketEventListenerTest {
    private lateinit var sessionRegistry: WebSocketSessionRegistry
    private lateinit var listener: WebSocketEventListener

    @BeforeEach
    fun setUp() {
        sessionRegistry = mockk(relaxed = true)
        listener = WebSocketEventListener(sessionRegistry)
    }

    @Nested
    @DisplayName("handleSessionDisconnect")
    inner class HandleSessionDisconnect {
        @Test
        fun `should remove session from registry on disconnect`() {
            // given
            val accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT)
            accessor.sessionId = "test-session-id"
            val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
            val event = SessionDisconnectEvent(this, message, "test-session-id", mockk())

            // when
            listener.handleSessionDisconnect(event)

            // then
            verify { sessionRegistry.remove("test-session-id") }
        }
    }
}
