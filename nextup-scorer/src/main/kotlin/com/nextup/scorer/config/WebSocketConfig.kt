package com.nextup.scorer.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket 설정
 *
 * 실시간 스코어보드 브로드캐스트를 위한 STOMP over WebSocket 설정
 * JWT 인증 필수, 허용된 도메인만 접근 가능
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthInterceptor: WebSocketAuthInterceptor,
    @Value("\${app.websocket.allowed-origins:http://localhost:3000}")
    private val allowedOrigins: String,
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // 클라이언트가 구독할 수 있는 prefix
        // /topic/games/{gameId}/scoreboard - 특정 경기 스코어보드
        // /topic/games/{gameId}/events - 특정 경기 이벤트
        registry.enableSimpleBroker("/topic")

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val origins = allowedOrigins.split(",").map { it.trim() }.toTypedArray()

        // WebSocket 연결 엔드포인트 (SockJS 포함)
        registry.addEndpoint("/ws/scoreboard")
            .setAllowedOrigins(*origins)
            .withSockJS()

        // SockJS 없이 순수 WebSocket 연결
        registry.addEndpoint("/ws/scoreboard")
            .setAllowedOrigins(*origins)
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(webSocketAuthInterceptor)
    }
}
