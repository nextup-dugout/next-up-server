package com.nextup.scorer.config

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus

/**
 * WebSocket 세션 JWT 만료 스케줄러
 *
 * 주기적으로 활성 WebSocket 세션의 JWT 토큰 만료 여부를 검사합니다.
 * 만료된 세션은 커스텀 CloseStatus(4401)로 연결을 해제합니다.
 *
 * 실행 주기: 60초 (fixedDelay)
 * - SEND/SUBSCRIBE 없이 유휴 상태인 세션도 만료 감지 가능
 * - 네트워크 비용 최소화를 위해 하트비트 주기(10초)보다 긴 간격 사용
 */
@Component
class WebSocketSessionExpirationScheduler(
    private val sessionRegistry: WebSocketSessionRegistry,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CHECK_INTERVAL_MS = 60_000L

        /** JWT 만료로 인한 WebSocket 종료 코드 (4000번대는 애플리케이션 전용) */
        val JWT_EXPIRED_CLOSE_STATUS =
            CloseStatus(4401, "JWT 토큰이 만료되었습니다. 재연결이 필요합니다.")
    }

    @Scheduled(fixedDelay = CHECK_INTERVAL_MS)
    fun checkExpiredSessions() {
        val allTokens = sessionRegistry.getAllTokens()
        if (allTokens.isEmpty()) {
            return
        }

        var expiredCount = 0
        for ((sessionId, token) in allTokens) {
            if (!jwtTokenProvider.validateToken(token)) {
                log.info("유휴 WebSocket 세션 JWT 만료 감지: sessionId={}", sessionId)
                disconnectSession(sessionId)
                expiredCount++
            }
        }

        if (expiredCount > 0) {
            log.info("만료된 WebSocket 세션 {}건 종료 처리 완료", expiredCount)
        }
    }

    private fun disconnectSession(sessionId: String) {
        try {
            val session = sessionRegistry.getWebSocketSession(sessionId)
            if (session != null && session.isOpen) {
                session.close(JWT_EXPIRED_CLOSE_STATUS)
                log.debug("WebSocket 세션 종료 완료: sessionId={}", sessionId)
            }
            sessionRegistry.remove(sessionId)
        } catch (e: Exception) {
            log.warn(
                "WebSocket 세션 종료 실패: sessionId={}, error={}",
                sessionId,
                e.message,
            )
            // 종료에 실패해도 레지스트리에서는 제거
            sessionRegistry.remove(sessionId)
        }
    }
}
