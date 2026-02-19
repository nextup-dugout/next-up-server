package com.nextup.infrastructure.security

import com.nextup.common.exception.*
import com.nextup.core.domain.auth.RefreshToken
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.repository.auth.RefreshTokenRepository
import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import com.nextup.infrastructure.security.oauth2.AuthCodeResult
import com.nextup.infrastructure.security.oauth2.AuthCodeStore
import com.nextup.infrastructure.security.userdetails.UserJpaRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

@DisplayName("AuthenticationService")
class AuthenticationServiceTest {
    private lateinit var userJpaRepository: UserJpaRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var authCodeStore: AuthCodeStore
    private lateinit var authenticationService: AuthenticationService

    @BeforeEach
    fun setUp() {
        userJpaRepository = mockk()
        refreshTokenRepository = mockk()
        jwtTokenProvider = mockk()
        passwordEncoder = mockk()
        authCodeStore = mockk()
        authenticationService =
            AuthenticationService(
                userJpaRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                passwordEncoder,
                authCodeStore,
            )
    }

    @Nested
    @DisplayName("login")
    inner class Login {
        @Test
        fun `should return token pair on successful login`() {
            // given
            val email = "test@example.com"
            val password = "password123"
            val encodedPassword = "encoded_password"
            val user = createTestUser(1L, email, encodedPassword)

            every { userJpaRepository.findByEmail(email) } returns user
            every { passwordEncoder.matches(password, encodedPassword) } returns true
            every { jwtTokenProvider.createAccessToken(any(), any(), any()) } returns "access_token"
            every { jwtTokenProvider.createRefreshToken(any()) } returns "refresh_token"
            every { jwtTokenProvider.getRefreshTokenExpiration() } returns Instant.now().plusSeconds(3600)
            every { refreshTokenRepository.save(any()) } answers { firstArg() }

            // when
            val result = authenticationService.login(email, password)

            // then
            assertThat(result.accessToken).isEqualTo("access_token")
            assertThat(result.refreshToken).isEqualTo("refresh_token")
            verify { refreshTokenRepository.save(any()) }
        }

        @Test
        fun `should throw InvalidCredentialsException when email not found`() {
            // given
            val email = "notfound@example.com"
            every { userJpaRepository.findByEmail(email) } returns null

            // when & then
            assertThatThrownBy {
                authenticationService.login(email, "password")
            }.isInstanceOf(InvalidCredentialsException::class.java)
        }

        @Test
        fun `should throw InvalidCredentialsException when password is wrong`() {
            // given
            val email = "test@example.com"
            val user = createTestUser(1L, email, "encoded_password")

            every { userJpaRepository.findByEmail(email) } returns user
            every { passwordEncoder.matches(any(), any()) } returns false

            // when & then
            assertThatThrownBy {
                authenticationService.login(email, "wrong_password")
            }.isInstanceOf(InvalidCredentialsException::class.java)
        }

        @Test
        fun `should throw UserDeactivatedException when user is inactive`() {
            // given
            val email = "test@example.com"
            val user = createTestUser(1L, email, "encoded_password").apply { deactivate() }

            every { userJpaRepository.findByEmail(email) } returns user

            // when & then
            assertThatThrownBy {
                authenticationService.login(email, "password")
            }.isInstanceOf(UserDeactivatedException::class.java)
        }

        @Test
        fun `should throw InvalidCredentialsException when user has no password (OAuth only)`() {
            // given
            val email = "oauth@example.com"
            val user = createOAuthUser(1L, email)

            every { userJpaRepository.findByEmail(email) } returns user

            // when & then
            assertThatThrownBy {
                authenticationService.login(email, "password")
            }.isInstanceOf(InvalidCredentialsException::class.java)
        }
    }

    @Nested
    @DisplayName("refresh")
    inner class Refresh {
        @Test
        fun `should return new token pair on successful refresh`() {
            // given
            val refreshTokenString = "valid_refresh_token"
            val refreshToken = createRefreshToken(1L, refreshTokenString)
            val user = createTestUser(1L, "test@example.com", "password")

            every { refreshTokenRepository.findByToken(refreshTokenString) } returns refreshToken
            every { refreshTokenRepository.save(any()) } answers { firstArg() }
            every { userJpaRepository.findById(1L) } returns Optional.of(user)
            every { jwtTokenProvider.createAccessToken(any(), any(), any()) } returns "new_access_token"
            every { jwtTokenProvider.createRefreshToken(any()) } returns "new_refresh_token"
            every { jwtTokenProvider.getRefreshTokenExpiration() } returns Instant.now().plusSeconds(3600)

            // when
            val result = authenticationService.refresh(refreshTokenString)

            // then
            assertThat(result.accessToken).isEqualTo("new_access_token")
            assertThat(result.refreshToken).isEqualTo("new_refresh_token")
            verify { refreshTokenRepository.save(any()) }
        }

        @Test
        fun `should throw RefreshTokenNotFoundException when token not found`() {
            // given
            every { refreshTokenRepository.findByToken(any()) } returns null

            // when & then
            assertThatThrownBy {
                authenticationService.refresh("invalid_token")
            }.isInstanceOf(RefreshTokenNotFoundException::class.java)
        }

        @Test
        fun `should throw RefreshTokenExpiredException when token is expired`() {
            // given
            val expiredToken = createExpiredRefreshToken(1L, "expired_token")
            every { refreshTokenRepository.findByToken("expired_token") } returns expiredToken

            // when & then
            assertThatThrownBy {
                authenticationService.refresh("expired_token")
            }.isInstanceOf(RefreshTokenExpiredException::class.java)
        }

        @Test
        fun `should throw RefreshTokenRevokedException when token is revoked`() {
            // given
            val revokedToken = createRevokedRefreshToken(1L, "revoked_token")
            every { refreshTokenRepository.findByToken("revoked_token") } returns revokedToken

            // when & then
            assertThatThrownBy {
                authenticationService.refresh("revoked_token")
            }.isInstanceOf(RefreshTokenRevokedException::class.java)
        }

        @Test
        fun `should throw UserDeactivatedException when user is inactive`() {
            // given
            val refreshTokenString = "valid_token"
            val refreshToken = createRefreshToken(1L, refreshTokenString)
            val inactiveUser = createTestUser(1L, "test@example.com", "password").apply { deactivate() }

            every { refreshTokenRepository.findByToken(refreshTokenString) } returns refreshToken
            every { refreshTokenRepository.save(any()) } answers { firstArg() }
            every { userJpaRepository.findById(1L) } returns Optional.of(inactiveUser)

            // when & then
            assertThatThrownBy {
                authenticationService.refresh(refreshTokenString)
            }.isInstanceOf(UserDeactivatedException::class.java)
        }
    }

    @Nested
    @DisplayName("logout")
    inner class Logout {
        @Test
        fun `should revoke refresh token on logout`() {
            // given
            val refreshTokenString = "valid_token"
            val refreshToken = createRefreshToken(1L, refreshTokenString)

            every { refreshTokenRepository.findByToken(refreshTokenString) } returns refreshToken
            every { refreshTokenRepository.save(any()) } answers { firstArg() }

            // when
            authenticationService.logout(refreshTokenString)

            // then
            verify { refreshTokenRepository.save(any()) }
        }

        @Test
        fun `should do nothing when token not found`() {
            // given
            every { refreshTokenRepository.findByToken(any()) } returns null

            // when - should not throw
            authenticationService.logout("nonexistent_token")

            // then
            verify(exactly = 0) { refreshTokenRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("logoutAll")
    inner class LogoutAll {
        @Test
        fun `should revoke all refresh tokens for user`() {
            // given
            val userId = 1L
            every { refreshTokenRepository.revokeAllByUserId(userId) } just Runs

            // when
            authenticationService.logoutAll(userId)

            // then
            verify { refreshTokenRepository.revokeAllByUserId(userId) }
        }
    }

    @Nested
    @DisplayName("getUserDetails")
    inner class GetUserDetails {
        @Test
        fun `should return user details when user found`() {
            // given
            val user = createTestUser(1L, "test@example.com", "password")
            every { userJpaRepository.findById(1L) } returns Optional.of(user)

            // when
            val result = authenticationService.getUserDetails(1L)

            // then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.email).isEqualTo("test@example.com")
        }

        @Test
        fun `should throw UserNotFoundException when user not found`() {
            // given
            every { userJpaRepository.findById(999L) } returns Optional.empty()

            // when & then
            assertThatThrownBy {
                authenticationService.getUserDetails(999L)
            }.isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("exchangeOAuth2Code")
    inner class ExchangeOAuth2Code {
        @Test
        fun `should return OAuth2TokenResult on valid code`() {
            // given
            val code = "valid-auth-code"
            val user = createTestUser(1L, "test@example.com", "password")

            every { authCodeStore.consume(code) } returns AuthCodeResult(userId = 1L, isNewUser = false)
            every { userJpaRepository.findById(1L) } returns Optional.of(user)
            every { jwtTokenProvider.createAccessToken(any(), any(), any()) } returns "access_token"
            every { jwtTokenProvider.createRefreshToken(any()) } returns "refresh_token"
            every { jwtTokenProvider.getRefreshTokenExpiration() } returns Instant.now().plusSeconds(3600)
            every { refreshTokenRepository.save(any()) } answers { firstArg() }

            // when
            val result = authenticationService.exchangeOAuth2Code(code)

            // then
            assertThat(result.accessToken).isEqualTo("access_token")
            assertThat(result.refreshToken).isEqualTo("refresh_token")
            assertThat(result.isNewUser).isFalse()
        }

        @Test
        fun `should return isNewUser true for new users`() {
            // given
            val code = "new-user-code"
            val user = createTestUser(1L, "new@example.com", "password")

            every { authCodeStore.consume(code) } returns AuthCodeResult(userId = 1L, isNewUser = true)
            every { userJpaRepository.findById(1L) } returns Optional.of(user)
            every { jwtTokenProvider.createAccessToken(any(), any(), any()) } returns "access_token"
            every { jwtTokenProvider.createRefreshToken(any()) } returns "refresh_token"
            every { jwtTokenProvider.getRefreshTokenExpiration() } returns Instant.now().plusSeconds(3600)
            every { refreshTokenRepository.save(any()) } answers { firstArg() }

            // when
            val result = authenticationService.exchangeOAuth2Code(code)

            // then
            assertThat(result.isNewUser).isTrue()
        }

        @Test
        fun `should throw InvalidInputException on invalid code`() {
            // given
            every { authCodeStore.consume("invalid-code") } returns null

            // when & then
            assertThatThrownBy {
                authenticationService.exchangeOAuth2Code("invalid-code")
            }.isInstanceOf(InvalidInputException::class.java)
                .hasMessageContaining("유효하지 않거나 만료된 인가 코드입니다")
        }

        @Test
        fun `should throw UserNotFoundException when user not found`() {
            // given
            every { authCodeStore.consume("valid-code") } returns AuthCodeResult(userId = 999L, isNewUser = false)
            every { userJpaRepository.findById(999L) } returns Optional.empty()

            // when & then
            assertThatThrownBy {
                authenticationService.exchangeOAuth2Code("valid-code")
            }.isInstanceOf(UserNotFoundException::class.java)
        }
    }

    // Helper methods
    private fun createTestUser(
        id: Long,
        email: String,
        password: String,
    ): User {
        val user =
            User.createLocalUser(
                email = email,
                encodedPassword = password,
                nickname = "테스터",
            )
        setUserId(user, id)
        return user
    }

    private fun createOAuthUser(
        id: Long,
        email: String,
    ): User {
        val user =
            User.createOAuthUser(
                email = email,
                nickname = "OAuth테스터",
                provider = com.nextup.core.domain.user.OAuthProvider.KAKAO,
                oauthId = "kakao_123",
            )
        setUserId(user, id)
        return user
    }

    private fun setUserId(
        user: User,
        id: Long,
    ) {
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
    }

    private fun createRefreshToken(
        userId: Long,
        token: String,
    ): RefreshToken =
        RefreshToken.create(
            userId = userId,
            token = token,
            expiresAt = Instant.now().plusSeconds(3600),
        )

    private fun createExpiredRefreshToken(
        userId: Long,
        token: String,
    ): RefreshToken {
        // Use reflection to create expired token since create() doesn't allow past expiration
        val constructor =
            RefreshToken::class.java.getDeclaredConstructor(
                Long::class.java,
                String::class.java,
                Instant::class.java,
                Boolean::class.java,
                String::class.java,
                String::class.java,
                Long::class.java,
            )
        constructor.isAccessible = true
        return constructor.newInstance(
            userId,
            token,
            Instant.now().minusSeconds(3600),
            false,
            null,
            null,
            0L,
        )
    }

    private fun createRevokedRefreshToken(
        userId: Long,
        token: String,
    ): RefreshToken {
        val refreshToken =
            RefreshToken.create(
                userId = userId,
                token = token,
                expiresAt = Instant.now().plusSeconds(3600),
            )
        refreshToken.revoke()
        return refreshToken
    }
}
