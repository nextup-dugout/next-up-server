package com.nextup.scorer.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket 설정
 *
 * 실시간 스코어보드 브로드캐스트를 위한 STOMP over WebSocket 설정
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // 클라이언트가 구독할 수 있는 prefix
        // /topic/games/{gameId}/scoreboard - 특정 경기 스코어보드
        // /topic/games/{gameId}/events - 특정 경기 이벤트
        registry.enableSimpleBroker("/topic")

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws/scoreboard")
            .setAllowedOriginPatterns("*")
            .withSockJS()

        // SockJS 없이 순수 WebSocket 연결
        registry.addEndpoint("/ws/scoreboard")
            .setAllowedOriginPatterns("*")
    }
}
