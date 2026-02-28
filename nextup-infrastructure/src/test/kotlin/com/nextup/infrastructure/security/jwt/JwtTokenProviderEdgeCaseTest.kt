package com.nextup.infrastructure.security.jwt

import com.nextup.common.exception.InvalidTokenException
import com.nextup.common.exception.TokenExpiredException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * JwtTokenProvider 엣지 케이스 및 추가 시나리오 테스트
 *
 * 기존 JwtTokenProviderTest에서 다루지 않은 시나리오를 검증합니다:
 * - 이메일 추출 (getEmail)
 * - 다양한 역할 조합
 * - 만료된 토큰에서의 다양한 메서드 호출
 * - 토큰 타입 상호 배타성
 */
@DisplayName("JwtTokenProvider 엣지 케이스 테스트")
class JwtTokenProviderEdgeCaseTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private val testSecret =
        "dGhpcyBpcyBhIHNhbXBsZSBiYXNlNjQgZW5jb2RlZCBzZWNyZXQga2V5IGZvciBkZXZlbG9wbWVudCBvbmx5"
    private val testIssuer = "nextup"

    @BeforeEach
    fun setUp() {
        val jwtProperties =
            JwtProperties(
                secret = testSecret,
                accessTokenExpiration = 1_800_000L,
                refreshTokenExpiration = 604_800_000L,
                issuer = testIssuer,
            )
        jwtTokenProvider = JwtTokenProvider(jwtProperties)
    }

    @Nested
    @DisplayName("이메일 추출 (getEmail)")
    inner class GetEmail {
        @Test
        fun `should extract email from access token`() {
            // given
            val email = "user@nextup.com"
            val token =
                jwtTokenProvider.createAccessToken(1L, email, setOf("USER"))

            // when
            val extracted = jwtTokenProvider.getEmail(token)

            // then
            assertThat(extracted).isEqualTo(email)
        }

        @Test
        fun `should extract email with special characters`() {
            // given
            val email = "user+tag@example.co.kr"
            val token =
                jwtTokenProvider.createAccessToken(42L, email, setOf("USER"))

            // when
            val extracted = jwtTokenProvider.getEmail(token)

            // then
            assertThat(extracted).isEqualTo(email)
        }

        @Test
        fun `should throw TokenExpiredException when getting email from expired token`() {
            // given
            val expiredProperties =
                JwtProperties(
                    secret = testSecret,
                    accessTokenExpiration = -1_000L,
                    refreshTokenExpiration = 604_800_000L,
                    issuer = testIssuer,
                )
            val expiredProvider = JwtTokenProvider(expiredProperties)
            val expiredToken =
                expiredProvider.createAccessToken(1L, "test@example.com", setOf("USER"))

            // when & then
            assertThatThrownBy {
                jwtTokenProvider.getEmail(expiredToken)
            }.isInstanceOf(TokenExpiredException::class.java)
        }
    }

    @Nested
    @DisplayName("역할(Role) 처리")
    inner class RoleHandling {
        @Test
        fun `should handle single role correctly`() {
            // given
            val token =
                jwtTokenProvider.createAccessToken(1L, "test@example.com", setOf("USER"))

            // when
            val roles = jwtTokenProvider.getRoles(token)

            // then
            assertThat(roles).containsExactly("USER")
        }

        @Test
        fun `should handle all predefined roles`() {
            // given
            val allRoles = setOf("USER", "ADMIN", "SCORER", "TEAM_MANAGER", "ASSOCIATION_ADMIN")
            val token =
                jwtTokenProvider.createAccessToken(1L, "admin@nextup.com", allRoles)

            // when
            val extracted = jwtTokenProvider.getRoles(token)

            // then
            assertThat(extracted).containsExactlyInAnyOrderElementsOf(allRoles)
        }

        @Test
        fun `should handle empty roles set in access token`() {
            // given
            val token =
                jwtTokenProvider.createAccessToken(1L, "test@example.com", emptySet())

            // when
            val roles = jwtTokenProvider.getRoles(token)

            // then
            assertThat(roles).isEmpty()
        }
    }

    @Nested
    @DisplayName("만료 토큰 처리")
    inner class ExpiredTokenHandling {
        private lateinit var expiredToken: String

        @BeforeEach
        fun createExpiredToken() {
            val expiredProperties =
                JwtProperties(
                    secret = testSecret,
                    accessTokenExpiration = -1_000L,
                    refreshTokenExpiration = 604_800_000L,
                    issuer = testIssuer,
                )
            val expiredProvider = JwtTokenProvider(expiredProperties)
            expiredToken =
                expiredProvider.createAccessToken(99L, "expired@example.com", setOf("USER"))
        }

        @Test
        fun `should return false when validating expired token`() {
            // when
            val isValid = jwtTokenProvider.validateToken(expiredToken)

            // then
            assertThat(isValid).isFalse()
        }

        @Test
        fun `should throw TokenExpiredException when extracting userId from expired token`() {
            assertThatThrownBy {
                jwtTokenProvider.getUserId(expiredToken)
            }.isInstanceOf(TokenExpiredException::class.java)
                .hasMessageContaining("expired")
        }

        @Test
        fun `should throw TokenExpiredException when extracting roles from expired token`() {
            assertThatThrownBy {
                jwtTokenProvider.getRoles(expiredToken)
            }.isInstanceOf(TokenExpiredException::class.java)
        }
    }

    @Nested
    @DisplayName("토큰 타입 상호 배타성")
    inner class TokenTypeMutualExclusivity {
        @Test
        fun `access token should not be identified as refresh token`() {
            // given
            val accessToken =
                jwtTokenProvider.createAccessToken(1L, "test@example.com", setOf("USER"))

            // then
            assertThat(jwtTokenProvider.isAccessToken(accessToken)).isTrue()
            assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse()
        }

        @Test
        fun `refresh token should not be identified as access token`() {
            // given
            val refreshToken = jwtTokenProvider.createRefreshToken(1L)

            // then
            assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue()
            assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse()
        }
    }

    @Nested
    @DisplayName("변조 토큰 처리")
    inner class TamperedTokenHandling {
        @Test
        fun `should return false for token with tampered payload`() {
            // given - base64 payload 부분을 변조
            val validToken =
                jwtTokenProvider.createAccessToken(1L, "test@example.com", setOf("USER"))
            val parts = validToken.split(".")
            val tamperedPayload = "eyJzdWIiOiI5OTkiLCJlbWFpbCI6ImhhY2tlckBleGFtcGxlLmNvbSJ9"
            val tamperedToken = "${parts[0]}.$tamperedPayload.${parts[2]}"

            // when
            val isValid = jwtTokenProvider.validateToken(tamperedToken)

            // then
            assertThat(isValid).isFalse()
        }

        @Test
        fun `should throw InvalidTokenException for null bytes in token`() {
            // given
            val nullToken = "\u0000\u0000\u0000"

            // when & then
            assertThatThrownBy {
                jwtTokenProvider.getUserId(nullToken)
            }.isInstanceOf(InvalidTokenException::class.java)
        }
    }

    @Nested
    @DisplayName("Refresh Token 만료 시간")
    inner class RefreshTokenExpiration {
        @Test
        fun `should return expiration further in future for longer refresh expiration config`() {
            // given
            val longExpProperties =
                JwtProperties(
                    secret = testSecret,
                    accessTokenExpiration = 1_800_000L,
                    refreshTokenExpiration = 2_592_000_000L, // 30일
                    issuer = testIssuer,
                )
            val longExpProvider = JwtTokenProvider(longExpProperties)

            val shortExpProperties =
                JwtProperties(
                    secret = testSecret,
                    accessTokenExpiration = 1_800_000L,
                    refreshTokenExpiration = 604_800_000L, // 7일
                    issuer = testIssuer,
                )
            val shortExpProvider = JwtTokenProvider(shortExpProperties)

            // when
            val longExp = longExpProvider.getRefreshTokenExpiration()
            val shortExp = shortExpProvider.getRefreshTokenExpiration()

            // then
            assertThat(longExp).isAfter(shortExp)
        }
    }
}
