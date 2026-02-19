package com.nextup.scorer.config

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

/**
 * WebSocket STOMP CONNECT 시 JWT 토큰을 검증하는 인터셉터
 *
 * Authorization 헤더에서 Bearer 토큰을 추출하여 인증을 수행합니다.
 */
@Component
class WebSocketAuthInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
) : ChannelInterceptor {
    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*> {
        val accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                ?: return message

        if (accessor.command == StompCommand.CONNECT) {
            val token =
                accessor.getFirstNativeHeader("Authorization")
                    ?.removePrefix(BEARER_PREFIX)
                    ?: throw AuthenticationCredentialsNotFoundException(
                        "WebSocket 연결에 인증 토큰이 필요합니다",
                    )

            if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
                throw AuthenticationCredentialsNotFoundException("유효하지 않은 토큰입니다")
            }

            val userId = jwtTokenProvider.getUserId(token)
            val roles = jwtTokenProvider.getRoles(token)
            val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }

            accessor.user =
                UsernamePasswordAuthenticationToken(userId, null, authorities)
        }

        return message
    }
}
