package com.nextup.infrastructure.security.jwt

import com.nextup.common.exception.InvalidTokenException
import com.nextup.common.exception.TokenExpiredException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var jwtProperties: JwtProperties

    // Base64 encoded secret key for testing (at least 256 bits for HS256)
    private val testSecret = "dGhpcyBpcyBhIHNhbXBsZSBiYXNlNjQgZW5jb2RlZCBzZWNyZXQga2V5IGZvciBkZXZlbG9wbWVudCBvbmx5"
    private val testIssuer = "nextup"

    @BeforeEach
    fun setUp() {
        jwtProperties =
            JwtProperties(
                secret = testSecret,
                accessTokenExpiration = 1800000L, // 30 minutes
                refreshTokenExpiration = 604800000L, // 7 days
                issuer = testIssuer,
            )
        jwtTokenProvider = JwtTokenProvider(jwtProperties)
    }

    @Nested
    @DisplayName("createAccessToken")
    inner class CreateAccessToken {
        @Test
        fun `should create valid access token`() {
            // given
            val userId = 1L
            val email = "test@example.com"
            val roles = setOf("USER", "ADMIN")

            // when
            val token = jwtTokenProvider.createAccessToken(userId, email, roles)

            // then
            assertThat(token).isNotBlank()
            assertThat(jwtTokenProvider.validateToken(token)).isTrue()
            assertThat(jwtTokenProvider.isAccessToken(token)).isTrue()
        }

        @Test
        fun `should include correct claims in access token`() {
            // given
            val userId = 1L
            val email = "test@example.com"
            val roles = setOf("USER", "ADMIN")

            // when
            val token = jwtTokenProvider.createAccessToken(userId, email, roles)

            // then
            assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId)
            assertThat(jwtTokenProvider.getEmail(token)).isEqualTo(email)
            assertThat(jwtTokenProvider.getRoles(token)).containsExactlyInAnyOrder("USER", "ADMIN")
        }
    }

    @Nested
    @DisplayName("createRefreshToken")
    inner class CreateRefreshToken {
        @Test
        fun `should create valid refresh token`() {
            // given
            val userId = 1L

            // when
            val token = jwtTokenProvider.createRefreshToken(userId)

            // then
            assertThat(token).isNotBlank()
            assertThat(jwtTokenProvider.validateToken(token)).isTrue()
            assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue()
        }

        @Test
        fun `should include user id in refresh token`() {
            // given
            val userId = 1L

            // when
            val token = jwtTokenProvider.createRefreshToken(userId)

            // then
            assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId)
        }
    }

    @Nested
    @DisplayName("getUserId")
    inner class GetUserId {
        @Test
        fun `should extract user id from access token`() {
            // given
            val userId = 123L
            val token = jwtTokenProvider.createAccessToken(userId, "test@example.com", setOf("USER"))

            // when
            val extractedUserId = jwtTokenProvider.getUserId(token)

            // then
            assertThat(extractedUserId).isEqualTo(userId)
        }

        @Test
        fun `should extract user id from refresh token`() {
            // given
            val userId = 456L
            val token = jwtTokenProvider.createRefreshToken(userId)

            // when
            val extractedUserId = jwtTokenProvider.getUserId(token)

            // then
            assertThat(extractedUserId).isEqualTo(userId)
        }
    }

    @Nested
    @DisplayName("getRoles")
    inner class GetRoles {
        @Test
        fun `should extract roles from access token`() {
            // given
            val roles = setOf("USER", "ADMIN", "SCORER")
            val token = jwtTokenProvider.createAccessToken(1L, "test@example.com", roles)

            // when
            val extractedRoles = jwtTokenProvider.getRoles(token)

            // then
            assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(roles)
        }

        @Test
        fun `should return empty set when no roles in token`() {
            // given
            val token = jwtTokenProvider.createRefreshToken(1L)

            // when
            val extractedRoles = jwtTokenProvider.getRoles(token)

            // then
            assertThat(extractedRoles).isEmpty()
        }
    }

    @Nested
    @DisplayName("validateToken")
    inner class ValidateToken {
        @Test
        fun `should return true for valid token`() {
            // given
            val token = jwtTokenProvider.createAccessToken(1L, "test@example.com", setOf("USER"))

            // when
            val isValid = jwtTokenProvider.validateToken(token)

            // then
            assertThat(isValid).isTrue()
        }

        @Test
        fun `should return false for invalid token`() {
            // given
            val invalidToken = "invalid.token.here"

            // when
            val isValid = jwtTokenProvider.validateToken(invalidToken)

            // then
            assertThat(isValid).isFalse()
        }

        @Test
        fun `should return false for empty token`() {
            // when
            val isValid = jwtTokenProvider.validateToken("")

            // then
            assertThat(isValid).isFalse()
        }

        @Test
        fun `should return false for token signed with different key`() {
            // given
            val differentSecret = "ZGlmZmVyZW50IHNlY3JldCBrZXkgZm9yIHRlc3RpbmcgdGhhdCBpcyBhdCBsZWFzdCAyNTYgYml0cw=="
            val differentKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(differentSecret))

            val tampered =
                Jwts
                    .builder()
                    .subject("1")
                    .signWith(differentKey, Jwts.SIG.HS256)
                    .compact()

            // when
            val isValid = jwtTokenProvider.validateToken(tampered)

            // then
            assertThat(isValid).isFalse()
        }
    }

    @Nested
    @DisplayName("Token Expiration")
    inner class TokenExpiration {
        @Test
        fun `should throw TokenExpiredException for expired token`() {
            // given
            val expiredProperties =
                JwtProperties(
                    secret = testSecret,
                    accessTokenExpiration = -1000L, // already expired
                    refreshTokenExpiration = 604800000L,
                    issuer = testIssuer,
                )
            val providerWithExpiredToken = JwtTokenProvider(expiredProperties)
            val expiredToken = providerWithExpiredToken.createAccessToken(1L, "test@example.com", setOf("USER"))

            // when & then
            assertThatThrownBy {
                jwtTokenProvider.getUserId(expiredToken)
            }.isInstanceOf(TokenExpiredException::class.java)
        }
    }

    @Nested
    @DisplayName("Invalid Token Handling")
    inner class InvalidTokenHandling {
        @Test
        fun `should throw InvalidTokenException for malformed token`() {
            // given
            val malformedToken = "not.a.valid.jwt.token"

            // when & then
            assertThatThrownBy {
                jwtTokenProvider.getUserId(malformedToken)
            }.isInstanceOf(InvalidTokenException::class.java)
        }

        @Test
        fun `should throw InvalidTokenException for empty token`() {
            // when & then
            assertThatThrownBy {
                jwtTokenProvider.getUserId("")
            }.isInstanceOf(InvalidTokenException::class.java)
        }
    }

    @Nested
    @DisplayName("isAccessToken / isRefreshToken")
    inner class TokenTypeCheck {
        @Test
        fun `should correctly identify access token`() {
            // given
            val accessToken = jwtTokenProvider.createAccessToken(1L, "test@example.com", setOf("USER"))
            val refreshToken = jwtTokenProvider.createRefreshToken(1L)

            // then
            assertThat(jwtTokenProvider.isAccessToken(accessToken)).isTrue()
            assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse()
        }

        @Test
        fun `should correctly identify refresh token`() {
            // given
            val accessToken = jwtTokenProvider.createAccessToken(1L, "test@example.com", setOf("USER"))
            val refreshToken = jwtTokenProvider.createRefreshToken(1L)

            // then
            assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue()
            assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse()
        }
    }

    @Nested
    @DisplayName("getRefreshTokenExpiration")
    inner class GetRefreshTokenExpiration {
        @Test
        fun `should return future expiration time`() {
            // when
            val expiration = jwtTokenProvider.getRefreshTokenExpiration()

            // then
            assertThat(expiration).isAfter(Instant.now())
        }
    }
}
