package com.nextup.scorer.config

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
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
 * WebSocket STOMP 인터셉터
 *
 * CONNECT 시 JWT 토큰을 검증하여 인증을 수행합니다.
 * SEND/SUBSCRIBE 시 기존 인증 정보의 토큰 만료 여부를 재검증합니다.
 * JWT 만료 시 STOMP ERROR를 발생시켜 연결을 해제합니다.
 */
@Component
class WebSocketAuthInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
) : ChannelInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val TOKEN_HEADER = "token"
        private val COMMANDS_REQUIRING_AUTH =
            setOf(StompCommand.SEND, StompCommand.SUBSCRIBE)
    }

    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*> {
        val accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                ?: return message

        when (accessor.command) {
            StompCommand.CONNECT -> handleConnect(accessor)
            in COMMANDS_REQUIRING_AUTH -> handleAuthenticatedCommand(accessor)
            else -> Unit
        }

        return message
    }

    private fun handleConnect(accessor: StompHeaderAccessor) {
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

        // credentials에 토큰을 저장하여 이후 메시지에서 만료 검증에 사용
        accessor.user =
            UsernamePasswordAuthenticationToken(userId, token, authorities)

        // 세션 속성에 토큰 저장 (SEND/SUBSCRIBE에서 재검증용)
        accessor.sessionAttributes?.set(TOKEN_HEADER, token)
    }

    private fun handleAuthenticatedCommand(accessor: StompHeaderAccessor) {
        val token =
            accessor.sessionAttributes?.get(TOKEN_HEADER) as? String
                ?: return

        if (!jwtTokenProvider.validateToken(token)) {
            log.info(
                "WebSocket JWT 만료 감지, 세션 종료: sessionId={}",
                accessor.sessionId,
            )
            throw AuthenticationCredentialsNotFoundException(
                "JWT 토큰이 만료되었습니다. 재연결이 필요합니다.",
            )
        }
    }
}
