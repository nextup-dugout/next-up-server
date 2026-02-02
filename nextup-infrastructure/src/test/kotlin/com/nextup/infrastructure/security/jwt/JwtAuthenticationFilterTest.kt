package com.nextup.infrastructure.security.jwt

import com.nextup.common.exception.InvalidTokenException
import com.nextup.common.exception.TokenExpiredException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var filter: JwtAuthenticationFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = mockk()
        filter = JwtAuthenticationFilter(jwtTokenProvider)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        filterChain = mockk(relaxed = true)
        SecurityContextHolder.clearContext()
    }

    @Nested
    @DisplayName("토큰 추출")
    inner class ResolveToken {

        @Test
        fun `should pass filter when no Authorization header`() {
            // given - no Authorization header set

            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify { filterChain.doFilter(request, response) }
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }

        @Test
        fun `should pass filter when Authorization header does not start with Bearer`() {
            // given
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz")

            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify { filterChain.doFilter(request, response) }
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    inner class ValidateToken {

        @Test
        fun `should set authentication when valid access token`() {
            // given
            val token = "valid_access_token"
            request.addHeader("Authorization", "Bearer $token")
            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns true
            every { jwtTokenProvider.getUserId(token) } returns 1L
            every { jwtTokenProvider.getRoles(token) } returns setOf("USER", "ADMIN")

            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify { filterChain.doFilter(request, response) }
            val authentication = SecurityContextHolder.getContext().authentication
            assertThat(authentication).isNotNull
            assertThat(authentication.principal).isEqualTo(1L)
            assertThat(authentication.authorities.map { it.authority })
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN")
        }

        @Test
        fun `should not set authentication when token is refresh token`() {
            // given
            val token = "refresh_token"
            request.addHeader("Authorization", "Bearer $token")
            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns false

            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify { filterChain.doFilter(request, response) }
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }

        @Test
        fun `should not set authentication when token is invalid`() {
            // given
            val token = "invalid_token"
            request.addHeader("Authorization", "Bearer $token")
            every { jwtTokenProvider.validateToken(token) } returns false

            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify { filterChain.doFilter(request, response) }
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }
    }

    @Nested
    @DisplayName("예외 처리")
    inner class ExceptionHandling {

        @Test
        fun `should set exception attribute when token expired`() {
            // given
            val token = "expired_token"
            request.addHeader("Authorization", "Bearer $token")
            every { jwtTokenProvider.validateToken(token) } throws TokenExpiredException("Token has expired")

            // when
            filter.doFilter(request, response, filterChain)

            // then
            val exception = request.getAttribute("exception")
            assertThat(exception).isInstanceOf(TokenExpiredException::class.java)
            verify { filterChain.doFilter(request, response) }
        }

        @Test
        fun `should set exception attribute when token invalid`() {
            // given
            val token = "malformed_token"
            request.addHeader("Authorization", "Bearer $token")
            every { jwtTokenProvider.validateToken(token) } throws InvalidTokenException("Invalid token")

            // when
            filter.doFilter(request, response, filterChain)

            // then
            val exception = request.getAttribute("exception")
            assertThat(exception).isInstanceOf(InvalidTokenException::class.java)
            verify { filterChain.doFilter(request, response) }
        }
    }

    @Nested
    @DisplayName("Bearer 토큰 파싱")
    inner class BearerTokenParsing {

        @Test
        fun `should extract token from Bearer prefix`() {
            // given
            val token = "my_jwt_token"
            request.addHeader("Authorization", "Bearer $token")
            every { jwtTokenProvider.validateToken(token) } returns true
            every { jwtTokenProvider.isAccessToken(token) } returns true
            every { jwtTokenProvider.getUserId(token) } returns 42L
            every { jwtTokenProvider.getRoles(token) } returns setOf("SCORER")

            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify { jwtTokenProvider.validateToken(token) }
            val authentication = SecurityContextHolder.getContext().authentication
            assertThat(authentication?.principal).isEqualTo(42L)
        }

        @Test
        fun `should handle Bearer prefix case sensitivity`() {
            // given - "bearer" (lowercase) should not be recognized
            request.addHeader("Authorization", "bearer some_token")

            // when
            filter.doFilter(request, response, filterChain)

            // then - no token validation should occur
            verify(exactly = 0) { jwtTokenProvider.validateToken(any()) }
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }
    }
}
