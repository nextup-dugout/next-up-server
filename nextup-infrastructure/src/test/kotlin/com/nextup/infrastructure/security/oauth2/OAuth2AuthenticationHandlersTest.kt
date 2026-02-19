package com.nextup.infrastructure.security.oauth2

import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.User
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

@DisplayName("OAuth2 Authentication Handlers 테스트")
class OAuth2AuthenticationHandlersTest {
    @Nested
    @DisplayName("OAuth2AuthenticationSuccessHandler")
    inner class SuccessHandlerTest {
        private lateinit var authCodeStore: AuthCodeStore
        private lateinit var successHandler: OAuth2AuthenticationSuccessHandler
        private lateinit var request: HttpServletRequest
        private lateinit var response: HttpServletResponse
        private lateinit var authentication: Authentication

        private val redirectUri = "http://localhost:3000/oauth/callback"

        @BeforeEach
        fun setUp() {
            authCodeStore = mockk()
            successHandler =
                OAuth2AuthenticationSuccessHandler(
                    authCodeStore,
                    redirectUri,
                )
            request = mockk(relaxed = true)
            response = mockk(relaxed = true)
            authentication = mockk()
        }

        private fun createPrincipal(
            userId: Long,
            isNewUser: Boolean,
        ): OAuth2UserPrincipal {
            val user =
                User.createOAuthUser(
                    email = "test@example.com",
                    nickname = "테스터",
                    provider = OAuthProvider.KAKAO,
                    oauthId = "kakao_123",
                )
            val idField = User::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(user, userId)

            val oAuth2UserInfo = KakaoOAuth2UserInfo(mapOf("id" to 123L))
            return OAuth2UserPrincipal(user, oAuth2UserInfo, isNewUser)
        }

        @Test
        fun `should redirect with auth code instead of tokens`() {
            // given
            val principal = createPrincipal(userId = 1L, isNewUser = false)
            val redirectUrlSlot = slot<String>()

            every { authentication.principal } returns principal
            every { authCodeStore.generate(1L, false) } returns "test-auth-code"
            every { response.sendRedirect(capture(redirectUrlSlot)) } just Runs

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication)

            // then
            assertThat(redirectUrlSlot.captured).contains("code=test-auth-code")
            assertThat(redirectUrlSlot.captured).doesNotContain("accessToken")
            assertThat(redirectUrlSlot.captured).doesNotContain("refreshToken")

            verify(exactly = 1) { authCodeStore.generate(1L, false) }
        }

        @Test
        fun `should pass isNewUser flag to auth code store`() {
            // given
            val principal = createPrincipal(userId = 1L, isNewUser = true)

            every { authentication.principal } returns principal
            every { authCodeStore.generate(1L, true) } returns "new-user-code"
            every { response.sendRedirect(any()) } just Runs

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication)

            // then
            verify(exactly = 1) { authCodeStore.generate(1L, true) }
        }

        @Test
        fun `should redirect to correct URI with code parameter`() {
            // given
            val principal = createPrincipal(userId = 5L, isNewUser = false)
            val redirectUrlSlot = slot<String>()

            every { authentication.principal } returns principal
            every { authCodeStore.generate(5L, false) } returns "abc-123"
            every { response.sendRedirect(capture(redirectUrlSlot)) } just Runs

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication)

            // then
            assertThat(redirectUrlSlot.captured)
                .startsWith("http://localhost:3000/oauth/callback")
            assertThat(redirectUrlSlot.captured).contains("code=abc-123")
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
            val exception =
                OAuth2AuthenticationException(
                    OAuth2Error("invalid_token"),
                    "Token validation failed",
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
            val exception =
                OAuth2AuthenticationException(
                    OAuth2Error("error"),
                    "Error with special chars: &?=",
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
