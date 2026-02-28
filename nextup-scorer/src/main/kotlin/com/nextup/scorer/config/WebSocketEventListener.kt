package com.nextup.scorer.config

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

/**
 * WebSocket 세션 이벤트 리스너
 *
 * 클라이언트 연결/해제 이벤트를 처리합니다.
 * - 연결 시: 세션 정보 로깅
 * - 해제 시: 세션 정리 및 로깅
 */
@Component
class WebSocketEventListener {

    private val log = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    fun handleSessionConnected(event: SessionConnectedEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId
        val user = accessor.user?.name ?: "anonymous"
        log.info("WebSocket 연결: sessionId={}, user={}", sessionId, user)
    }

    @EventListener
    fun handleSessionDisconnect(event: SessionDisconnectEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId
        val user = accessor.user?.name ?: "anonymous"
        log.info("WebSocket 해제: sessionId={}, user={}", sessionId, user)
    }
}
