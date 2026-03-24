package com.nextup.scorer.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration
import org.springframework.web.socket.handler.WebSocketHandlerDecorator
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory

/**
 * WebSocket 설정
 *
 * 실시간 스코어보드 브로드캐스트를 위한 STOMP over WebSocket 설정
 * JWT 인증 필수, 허용된 도메인만 접근 가능
 * Heartbeat: 서버<->클라이언트 10초 간격
 * 세션 비활성 타임아웃: 35분 (JWT access token 만료 30분 + 버퍼 5분)
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthInterceptor: WebSocketAuthInterceptor,
    private val webSocketHandshakeInterceptor: WebSocketHandshakeInterceptor,
    private val sessionRegistry: WebSocketSessionRegistry,
    @Value("\${app.websocket.allowed-origins:http://localhost:3000}")
    private val allowedOrigins: String,
) : WebSocketMessageBrokerConfigurer {

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 10000L
        private const val SEND_BUFFER_SIZE_LIMIT = 512 * 1024
        private const val SEND_TIME_LIMIT_MS = 20_000
    }

    @Bean
    fun heartbeatScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("ws-heartbeat-")
            initialize()
        }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // 클라이언트가 구독할 수 있는 prefix
        // /topic/games/{gameId}/scoreboard - 특정 경기 스코어보드
        // /topic/games/{gameId}/events - 특정 경기 이벤트
        registry.enableSimpleBroker("/topic")
            .setHeartbeatValue(longArrayOf(HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS))
            .setTaskScheduler(heartbeatScheduler())

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val origins = allowedOrigins.split(",").map { it.trim() }.toTypedArray()

        // WebSocket 연결 엔드포인트 (SockJS 포함)
        // 핸드셰이크 인터셉터로 HTTP 레벨에서 JWT 검증 (defense-in-depth)
        registry.addEndpoint("/ws/scoreboard")
            .addInterceptors(webSocketHandshakeInterceptor)
            .setAllowedOrigins(*origins)
            .withSockJS()

        // SockJS 없이 순수 WebSocket 연결
        registry.addEndpoint("/ws/scoreboard")
            .addInterceptors(webSocketHandshakeInterceptor)
            .setAllowedOrigins(*origins)
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(webSocketAuthInterceptor)
    }

    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
        registration
            .setSendBufferSizeLimit(SEND_BUFFER_SIZE_LIMIT)
            .setSendTimeLimit(SEND_TIME_LIMIT_MS)
            .addDecoratorFactory(sessionTrackingDecoratorFactory())
    }

    /**
     * WebSocketSession 참조를 레지스트리에 등록/해제하는 데코레이터 팩토리.
     * Transport 레벨에서 세션의 생명주기를 추적합니다.
     */
    private fun sessionTrackingDecoratorFactory(): WebSocketHandlerDecoratorFactory =
        WebSocketHandlerDecoratorFactory { handler ->
            object : WebSocketHandlerDecorator(handler) {
                override fun afterConnectionEstablished(session: WebSocketSession) {
                    sessionRegistry.registerSession(session)
                    super.afterConnectionEstablished(session)
                }

                override fun afterConnectionClosed(
                    session: WebSocketSession,
                    closeStatus: CloseStatus,
                ) {
                    sessionRegistry.remove(session.id)
                    super.afterConnectionClosed(session, closeStatus)
                }
            }
        }
}
