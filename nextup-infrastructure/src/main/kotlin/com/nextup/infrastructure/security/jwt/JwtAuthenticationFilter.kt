package com.nextup.infrastructure.security.jwt

import com.nextup.common.exception.InvalidTokenException
import com.nextup.common.exception.TokenExpiredException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 인증을 처리하는 필터
 *
 * Authorization 헤더에서 Bearer 토큰을 추출하여 인증을 수행합니다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val token = resolveToken(request)

            if (token != null && jwtTokenProvider.validateToken(token)) {
                if (jwtTokenProvider.isAccessToken(token)) {
                    val authentication = getAuthentication(token)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } catch (e: TokenExpiredException) {
            request.setAttribute("exception", e)
        } catch (e: InvalidTokenException) {
            request.setAttribute("exception", e)
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출합니다.
     *
     * @param request HTTP 요청
     * @return 토큰 문자열 (없으면 null)
     */
    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    /**
     * 토큰에서 인증 정보를 생성합니다.
     *
     * @param token JWT 토큰
     * @return Spring Security 인증 객체
     */
    private fun getAuthentication(token: String): UsernamePasswordAuthenticationToken {
        val userId = jwtTokenProvider.getUserId(token)
        val roles = jwtTokenProvider.getRoles(token)
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }

        return UsernamePasswordAuthenticationToken(
            userId,
            null,
            authorities,
        )
    }
}
