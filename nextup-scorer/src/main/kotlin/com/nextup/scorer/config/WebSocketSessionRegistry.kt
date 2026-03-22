package com.nextup.scorer.config

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket 세션 레지스트리
 *
 * 활성 WebSocket 세션의 JWT 토큰 및 WebSocketSession 참조를 관리합니다.
 * - CONNECT 시 토큰 등록
 * - Transport 연결 시 WebSocketSession 등록
 * - DISCONNECT 시 세션 제거
 * - 토큰 갱신 시 세션 토큰 업데이트
 * - 주기적 만료 검사를 위한 전체 세션 조회
 *
 * Thread-safe: ConcurrentHashMap 기반
 */
@Component
class WebSocketSessionRegistry {
    private val log = LoggerFactory.getLogger(javaClass)

    private val tokens = ConcurrentHashMap<String, String>()
    private val webSocketSessions = ConcurrentHashMap<String, WebSocketSession>()

    /**
     * 세션의 JWT 토큰을 등록합니다.
     *
     * @param sessionId WebSocket 세션 ID
     * @param token JWT 토큰
     */
    fun registerToken(
        sessionId: String,
        token: String,
    ) {
        tokens[sessionId] = token
        log.debug("WebSocket 세션 토큰 등록: sessionId={}", sessionId)
    }

    /**
     * WebSocketSession 참조를 등록합니다.
     *
     * @param session WebSocketSession
     */
    fun registerSession(session: WebSocketSession) {
        webSocketSessions[session.id] = session
        log.debug("WebSocket 세션 참조 등록: sessionId={}", session.id)
    }

    /**
     * 세션을 제거합니다.
     *
     * @param sessionId WebSocket 세션 ID
     */
    fun remove(sessionId: String) {
        tokens.remove(sessionId)
        webSocketSessions.remove(sessionId)
        log.debug("WebSocket 세션 제거: sessionId={}", sessionId)
    }

    /**
     * 세션의 토큰을 갱신합니다.
     *
     * @param sessionId WebSocket 세션 ID
     * @param newToken 갱신된 JWT 토큰
     * @return 세션이 존재하여 갱신에 성공하면 true
     */
    fun updateToken(
        sessionId: String,
        newToken: String,
    ): Boolean {
        val existed = tokens.replace(sessionId, newToken) != null
        if (existed) {
            log.debug("WebSocket 세션 토큰 갱신: sessionId={}", sessionId)
        }
        return existed
    }

    /**
     * 등록된 모든 세션 토큰의 스냅샷을 반환합니다.
     *
     * @return sessionId -> token 맵의 불변 복사본
     */
    fun getAllTokens(): Map<String, String> = tokens.toMap()

    /**
     * 특정 세션의 WebSocketSession 참조를 반환합니다.
     *
     * @param sessionId WebSocket 세션 ID
     * @return WebSocketSession (없으면 null)
     */
    fun getWebSocketSession(sessionId: String): WebSocketSession? =
        webSocketSessions[sessionId]

    /**
     * 등록된 세션 수를 반환합니다.
     */
    fun sessionCount(): Int = tokens.size
}
