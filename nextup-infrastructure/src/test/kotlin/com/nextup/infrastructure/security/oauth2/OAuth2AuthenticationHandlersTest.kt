package com.nextup.infrastructure.security.oauth2

import com.nextup.core.domain.auth.RefreshToken
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.repository.auth.RefreshTokenRepository
import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import com.nextup.infrastructure.security.oauth2.impl.KakaoOAuth2UserInfo
import io.mockk.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import java.time.Instant

@DisplayName("OAuth2 Authentication Handlers 테스트")
class OAuth2AuthenticationHandlersTest {

    @Nested
    @DisplayName("OAuth2AuthenticationSuccessHandler")
    inner class SuccessHandlerTest {

        private lateinit var jwtTokenProvider: JwtTokenProvider
        private lateinit var refreshTokenRepository: RefreshTokenRepository
        private lateinit var successHandler: OAuth2AuthenticationSuccessHandler
        private lateinit var request: HttpServletRequest
        private lateinit var response: HttpServletResponse
        private lateinit var authentication: Authentication

        private val redirectUri = "http://localhost:3000/oauth/callback"

        @BeforeEach
        fun setUp() {
            jwtTokenProvider = mockk()
            refreshTokenRepository = mockk(relaxed = true)
            successHandler = OAuth2AuthenticationSuccessHandler(
                jwtTokenProvider,
                refreshTokenRepository,
                redirectUri
            )
            request = mockk(relaxed = true)
            response = mockk(relaxed = true)
            authentication = mockk()
        }

        private fun createPrincipal(userId: Long, isNewUser: Boolean): OAuth2UserPrincipal {
            val user = User.createOAuthUser(
                email = "test@example.com",
                nickname = "테스터",
                provider = OAuthProvider.KAKAO,
                oauthId = "kakao_123"
            )
            val idField = User::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(user, userId)

            val oAuth2UserInfo = KakaoOAuth2UserInfo(mapOf("id" to 123L))
            return OAuth2UserPrincipal(user, oAuth2UserInfo, isNewUser)
        }

        @Test
        fun `should redirect with tokens on success`() {
            // given
            val principal = createPrincipal(userId = 1L, isNewUser = false)
            val redirectUrlSlot = slot<String>()

            every { authentication.principal } returns principal
            every { jwtTokenProvider.createAccessToken(any(), any(), any()) } returns "access-token"
            every { jwtTokenProvider.createRefreshToken(any()) } returns "refresh-token"
            every { jwtTokenProvider.getRefreshTokenExpiration() } returns Instant.now().plusSeconds(604800)
            every { request.getHeader("User-Agent") } returns "Mozilla/5.0"
            every { request.getHeader("X-Forwarded-For") } returns null
            every { request.remoteAddr } returns "127.0.0.1"
            every { refreshTokenRepository.save(any<RefreshToken>()) } returns mockk()
            every { response.sendRedirect(capture(redirectUrlSlot)) } just Runs

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication)

            // then
            verify { refreshTokenRepository.save(any<RefreshToken>()) }
            assertThat(redirectUrlSlot.captured).contains("accessToken=access-token")
            assertThat(redirectUrlSlot.captured).contains("refreshToken=refresh-token")
            assertThat(redirectUrlSlot.captured).contains("isNewUser=false")
        }

        @Test
        fun `should set isNewUser to true for new users`() {
            // given
            val principal = createPrincipal(userId = 1L, isNewUser = true)
            val redirectUrlSlot = slot<String>()

            every { authentication.principal } returns principal
            every { jwtTokenProvider.createAccessToken(any(), any(), any()) } returns "access-token"
            every { jwtTokenProvider.createRefreshToken(any()) } returns "refresh-token"
            every { jwtTokenProvider.getRefreshTokenExpiration() } returns Instant.now().plusSeconds(604800)
            every { request.getHeader("User-Agent") } returns "Mozilla/5.0"
            every { request.getHeader("X-Forwarded-For") } returns null
            every { request.remoteAddr } returns "127.0.0.1"
            every { refreshTokenRepository.save(any<RefreshToken>()) } returns mockk()
            every { response.sendRedirect(capture(redirectUrlSlot)) } just Runs

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication)

            // then
            assertThat(redirectUrlSlot.captured).contains("isNewUser=true")
        }

        @Test
        fun `should use X-Forwarded-For header for client IP`() {
            // given
            val principal = createPrincipal(userId = 1L, isNewUser = false)
            val refreshTokenSlot = slot<RefreshToken>()

            every { authentication.principal } returns principal
            every { jwtTokenProvider.createAccessToken(any(), any(), any()) } returns "access-token"
            every { jwtTokenProvider.createRefreshToken(any()) } returns "refresh-token"
            every { jwtTokenProvider.getRefreshTokenExpiration() } returns Instant.now().plusSeconds(604800)
            every { request.getHeader("User-Agent") } returns "Mozilla/5.0"
            every { request.getHeader("X-Forwarded-For") } returns "192.168.1.1, 10.0.0.1"
            every { refreshTokenRepository.save(capture(refreshTokenSlot)) } returns mockk()
            every { response.sendRedirect(any()) } just Runs

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication)

            // then
            assertThat(refreshTokenSlot.captured.ipAddress).isEqualTo("192.168.1.1")
        }

        @Test
        fun `should use remoteAddr when X-Forwarded-For is not present`() {
            // given
            val principal = createPrincipal(userId = 1L, isNewUser = false)
            val refreshTokenSlot = slot<RefreshToken>()

            every { authentication.principal } returns principal
            every { jwtTokenProvider.createAccessToken(any(), any(), any()) } returns "access-token"
            every { jwtTokenProvider.createRefreshToken(any()) } returns "refresh-token"
            every { jwtTokenProvider.getRefreshTokenExpiration() } returns Instant.now().plusSeconds(604800)
            every { request.getHeader("User-Agent") } returns "Mozilla/5.0"
            every { request.getHeader("X-Forwarded-For") } returns null
            every { request.remoteAddr } returns "10.0.0.5"
            every { refreshTokenRepository.save(capture(refreshTokenSlot)) } returns mockk()
            every { response.sendRedirect(any()) } just Runs

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication)

            // then
            assertThat(refreshTokenSlot.captured.ipAddress).isEqualTo("10.0.0.5")
        }
    }

    @Nested
    @DisplayName("OAuth2AuthenticationFailureHandler")
    inner class FailureHandlerTest {

        private lateinit var failureHandler: OAuth2AuthenticationFailureHandler
        private lateinit var request: HttpServletRequest
        private lateinit var response: HttpServletResponse

        private val redirectUri = "http://localhost:3000/oauth/callback"

        @BeforeEach
        fun setUp() {
            failureHandler = OAuth2AuthenticationFailureHandler(redirectUri)
            request = mockk(relaxed = true)
            response = mockk(relaxed = true)
        }

        @Test
        fun `should redirect with error on failure`() {
            // given
            val exception = OAuth2AuthenticationException(
                OAuth2Error("invalid_token"),
                "Token validation failed"
            )
            val redirectUrlSlot = slot<String>()
            every { response.sendRedirect(capture(redirectUrlSlot)) } just Runs

            // when
            failureHandler.onAuthenticationFailure(request, response, exception)

            // then
            assertThat(redirectUrlSlot.captured).contains("error=oauth_error")
            assertThat(redirectUrlSlot.captured).contains("message=")
        }

        @Test
        fun `should URL encode error message`() {
            // given
            val exception = OAuth2AuthenticationException(
                OAuth2Error("error"),
                "Error with special chars: &?="
            )
            val redirectUrlSlot = slot<String>()
            every { response.sendRedirect(capture(redirectUrlSlot)) } just Runs

            // when
            failureHandler.onAuthenticationFailure(request, response, exception)

            // then
            assertThat(redirectUrlSlot.captured).doesNotContain("Error with special chars: &?=")
            assertThat(redirectUrlSlot.captured).contains("Error+with+special+chars")
        }

        @Test
        fun `should use default message when exception message is null`() {
            // given
            val exception = OAuth2AuthenticationException(OAuth2Error("error"))
            val redirectUrlSlot = slot<String>()
            every { response.sendRedirect(capture(redirectUrlSlot)) } just Runs

            // when
            failureHandler.onAuthenticationFailure(request, response, exception)

            // then
            assertThat(redirectUrlSlot.captured).contains("message=")
        }
    }
}
