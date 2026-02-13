package com.nextup.api.controller.auth

import com.nextup.api.dto.auth.LoginRequest
import com.nextup.api.dto.auth.LogoutRequest
import com.nextup.api.dto.auth.RefreshTokenRequest
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.security.AuthenticationService
import com.nextup.infrastructure.security.TokenPair
import com.nextup.infrastructure.security.userdetails.CustomUserDetails
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AuthController 테스트")
class AuthControllerTest {
    private lateinit var authenticationService: AuthenticationService
    private lateinit var authController: AuthController
    private lateinit var httpServletRequest: HttpServletRequest

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        authController = AuthController(authenticationService)
        httpServletRequest = mockk()
    }

    @Nested
    @DisplayName("로그인")
    inner class Login {
        @Test
        fun `should return token pair on successful login`() {
            // given
            val request = LoginRequest(email = "test@example.com", password = "password123")
            val tokenPair =
                TokenPair(
                    accessToken = "access_token",
                    refreshToken = "refresh_token",
                )

            every { httpServletRequest.getHeader("User-Agent") } returns "Mozilla/5.0"
            every { httpServletRequest.getHeader("X-Forwarded-For") } returns null
            every { httpServletRequest.remoteAddr } returns "127.0.0.1"
            every {
                authenticationService.login(
                    email = "test@example.com",
                    password = "password123",
                    deviceInfo = "Mozilla/5.0",
                    ipAddress = "127.0.0.1",
                )
            } returns tokenPair

            // when
            val response = authController.login(request, httpServletRequest)

            // then
            assertThat(response.statusCode.value()).isEqualTo(200)
            assertThat(response.body?.data?.accessToken).isEqualTo("access_token")
            assertThat(response.body?.data?.refreshToken).isEqualTo("refresh_token")
            assertThat(response.body?.data?.tokenType).isEqualTo("Bearer")
        }

        @Test
        fun `should extract IP from X-Forwarded-For header`() {
            // given
            val request = LoginRequest(email = "test@example.com", password = "password123")
            val tokenPair = TokenPair(accessToken = "access", refreshToken = "refresh")

            every { httpServletRequest.getHeader("User-Agent") } returns "Chrome"
            every { httpServletRequest.getHeader("X-Forwarded-For") } returns "192.168.1.1, 10.0.0.1"
            every {
                authenticationService.login(
                    email = "test@example.com",
                    password = "password123",
                    deviceInfo = "Chrome",
                    ipAddress = "192.168.1.1",
                )
            } returns tokenPair

            // when
            val response = authController.login(request, httpServletRequest)

            // then
            assertThat(response.statusCode.value()).isEqualTo(200)
            verify {
                authenticationService.login(
                    email = "test@example.com",
                    password = "password123",
                    deviceInfo = "Chrome",
                    ipAddress = "192.168.1.1",
                )
            }
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    inner class Refresh {
        @Test
        fun `should return new token pair on successful refresh`() {
            // given
            val request = RefreshTokenRequest(refreshToken = "old_refresh_token")
            val tokenPair =
                TokenPair(
                    accessToken = "new_access_token",
                    refreshToken = "new_refresh_token",
                )

            every { httpServletRequest.getHeader("User-Agent") } returns "Safari"
            every { httpServletRequest.getHeader("X-Forwarded-For") } returns null
            every { httpServletRequest.remoteAddr } returns "10.0.0.1"
            every {
                authenticationService.refresh(
                    refreshTokenString = "old_refresh_token",
                    deviceInfo = "Safari",
                    ipAddress = "10.0.0.1",
                )
            } returns tokenPair

            // when
            val response = authController.refresh(request, httpServletRequest)

            // then
            assertThat(response.statusCode.value()).isEqualTo(200)
            assertThat(response.body?.data?.accessToken).isEqualTo("new_access_token")
            assertThat(response.body?.data?.refreshToken).isEqualTo("new_refresh_token")
        }
    }

    @Nested
    @DisplayName("로그아웃")
    inner class Logout {
        @Test
        fun `should logout successfully`() {
            // given
            val request = LogoutRequest(refreshToken = "refresh_token")
            every { authenticationService.logout("refresh_token") } just runs

            // when
            val response = authController.logout(request)

            // then
            assertThat(response.statusCode.value()).isEqualTo(200)
            assertThat(response.body?.success).isTrue()
            verify { authenticationService.logout("refresh_token") }
        }
    }

    @Nested
    @DisplayName("전체 로그아웃")
    inner class LogoutAll {
        @Test
        fun `should logout from all devices`() {
            // given
            val userId = 1L
            every { authenticationService.logoutAll(userId) } just runs

            // when
            val response = authController.logoutAll(userId)

            // then
            assertThat(response.statusCode.value()).isEqualTo(200)
            assertThat(response.body?.success).isTrue()
            verify { authenticationService.logoutAll(userId) }
        }
    }

    @Nested
    @DisplayName("현재 사용자 조회")
    inner class GetCurrentUser {
        @Test
        fun `should return current user info`() {
            // given
            val userId = 1L
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "encoded",
                    nickname = "테스터",
                )
            // Use reflection to set ID
            val idField = user.javaClass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(user, userId)

            val userDetails = CustomUserDetails.from(user)

            every { authenticationService.getUserDetails(userId) } returns userDetails

            // when
            val response = authController.getCurrentUser(userId)

            // then
            assertThat(response.statusCode.value()).isEqualTo(200)
            assertThat(response.body?.data?.id).isEqualTo(userId)
            assertThat(response.body?.data?.email).isEqualTo("test@example.com")
            assertThat(response.body?.data?.nickname).isEqualTo("테스터")
            assertThat(response.body?.data?.roles).contains("USER")
            assertThat(response.body?.data?.isActive).isTrue()
        }
    }
}
