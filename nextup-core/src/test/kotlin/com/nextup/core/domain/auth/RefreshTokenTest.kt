package com.nextup.core.domain.auth

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("RefreshToken Entity")
class RefreshTokenTest {

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        fun `should create RefreshToken with valid parameters`() {
            // given
            val userId = 1L
            val token = "test-refresh-token"
            val expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)

            // when
            val refreshToken = RefreshToken.create(
                userId = userId,
                token = token,
                expiresAt = expiresAt
            )

            // then
            assertThat(refreshToken.userId).isEqualTo(userId)
            assertThat(refreshToken.token).isEqualTo(token)
            assertThat(refreshToken.expiresAt).isEqualTo(expiresAt)
            assertThat(refreshToken.isRevoked).isFalse()
            assertThat(refreshToken.deviceInfo).isNull()
            assertThat(refreshToken.ipAddress).isNull()
        }

        @Test
        fun `should create RefreshToken with optional parameters`() {
            // given
            val userId = 1L
            val token = "test-refresh-token"
            val expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            val deviceInfo = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
            val ipAddress = "192.168.1.1"

            // when
            val refreshToken = RefreshToken.create(
                userId = userId,
                token = token,
                expiresAt = expiresAt,
                deviceInfo = deviceInfo,
                ipAddress = ipAddress
            )

            // then
            assertThat(refreshToken.deviceInfo).isEqualTo(deviceInfo)
            assertThat(refreshToken.ipAddress).isEqualTo(ipAddress)
        }

        @Test
        fun `should throw exception when token is blank`() {
            // given
            val userId = 1L
            val expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)

            // when & then
            assertThatThrownBy {
                RefreshToken.create(
                    userId = userId,
                    token = "   ",
                    expiresAt = expiresAt
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Token cannot be blank")
        }

        @Test
        fun `should throw exception when expiresAt is in the past`() {
            // given
            val userId = 1L
            val token = "test-refresh-token"
            val expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)

            // when & then
            assertThatThrownBy {
                RefreshToken.create(
                    userId = userId,
                    token = token,
                    expiresAt = expiresAt
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Expiration time must be in the future")
        }
    }

    @Nested
    @DisplayName("isExpired")
    inner class IsExpired {

        @Test
        fun `should return false when token is not expired`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )

            // when & then
            assertThat(refreshToken.isExpired).isFalse()
        }
    }

    @Nested
    @DisplayName("isValid")
    inner class IsValid {

        @Test
        fun `should return true when token is not expired and not revoked`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )

            // when & then
            assertThat(refreshToken.isValid).isTrue()
        }

        @Test
        fun `should return false when token is revoked`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )
            refreshToken.revoke()

            // when & then
            assertThat(refreshToken.isValid).isFalse()
        }
    }

    @Nested
    @DisplayName("revoke()")
    inner class Revoke {

        @Test
        fun `should revoke token`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )

            // when
            refreshToken.revoke()

            // then
            assertThat(refreshToken.isRevoked).isTrue()
        }

        @Test
        fun `should not change state when already revoked`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )
            refreshToken.revoke()

            // when
            refreshToken.revoke()

            // then
            assertThat(refreshToken.isRevoked).isTrue()
        }
    }

    @Nested
    @DisplayName("validate()")
    inner class Validate {

        @Test
        fun `should not throw when token is valid`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )

            // when & then
            refreshToken.validate() // should not throw
        }

        @Test
        fun `should throw when token is revoked`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )
            refreshToken.revoke()

            // when & then
            assertThatThrownBy { refreshToken.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("revoked")
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    inner class EqualsAndHashCode {

        @Test
        fun `should be equal when same reference`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )

            // when & then
            assertThat(refreshToken).isEqualTo(refreshToken)
        }

        @Test
        fun `should not be equal when id is 0`() {
            // given
            val token1 = RefreshToken.create(
                userId = 1L,
                token = "test-token-1",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )
            val token2 = RefreshToken.create(
                userId = 1L,
                token = "test-token-2",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )

            // when & then
            assertThat(token1).isNotEqualTo(token2)
        }

        @Test
        fun `should not be equal to different type`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )

            // when & then
            assertThat(refreshToken).isNotEqualTo("string")
        }
    }

    @Nested
    @DisplayName("toString()")
    inner class ToString {

        @Test
        fun `should return readable string representation`() {
            // given
            val refreshToken = RefreshToken.create(
                userId = 1L,
                token = "test-token",
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )

            // when
            val result = refreshToken.toString()

            // then
            assertThat(result).contains("RefreshToken")
            assertThat(result).contains("userId=1")
            assertThat(result).contains("isRevoked=false")
        }
    }
}
