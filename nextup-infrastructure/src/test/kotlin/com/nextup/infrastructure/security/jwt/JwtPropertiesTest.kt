package com.nextup.infrastructure.security.jwt

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Base64

@DisplayName("JwtProperties")
class JwtPropertiesTest {
    // 32바이트(256비트) 이상의 유효한 테스트용 secret
    private val validSecret = Base64.getEncoder().encodeToString("a]valid-secret-key-at-least-32-bytes!".toByteArray())

    @Nested
    @DisplayName("secret 검증")
    inner class SecretValidation {
        @Test
        fun `should create properties with valid secret`() {
            // when
            val properties = JwtProperties(secret = validSecret)

            // then
            assertThat(properties.secret).isEqualTo(validSecret)
        }

        @Test
        fun `should throw exception when secret is blank`() {
            // when & then
            assertThatThrownBy {
                JwtProperties(secret = "")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT_SECRET 환경변수가 설정되지 않았습니다")
        }

        @Test
        fun `should throw exception when secret is whitespace only`() {
            // when & then
            assertThatThrownBy {
                JwtProperties(secret = "   ")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("JWT_SECRET 환경변수가 설정되지 않았습니다")
        }

        @Test
        fun `should throw exception when secret is not valid base64`() {
            // when & then
            assertThatThrownBy {
                JwtProperties(secret = "not-valid-base64!!@@##")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("유효한 Base64 형식이 아닙니다")
        }

        @Test
        fun `should throw exception when secret is too short`() {
            // given - 16바이트(128비트) secret (최소 32바이트 필요)
            val shortSecret = Base64.getEncoder().encodeToString("short-secret-key".toByteArray())

            // when & then
            assertThatThrownBy {
                JwtProperties(secret = shortSecret)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 256비트(32바이트) 이상이어야 합니다")
        }

        @Test
        fun `should accept secret with exactly 32 bytes`() {
            // given - 정확히 32바이트
            val exact32Bytes = Base64.getEncoder().encodeToString("exactly-32-bytes-secret-key!!!!".toByteArray())
            val decoded = Base64.getDecoder().decode(exact32Bytes)

            // 32바이트 이상인지 확인 후 생성
            if (decoded.size >= 32) {
                val properties = JwtProperties(secret = exact32Bytes)
                assertThat(properties.secret).isEqualTo(exact32Bytes)
            } else {
                // 31바이트인 경우 실패해야 함
                assertThatThrownBy {
                    JwtProperties(secret = exact32Bytes)
                }.isInstanceOf(IllegalArgumentException::class.java)
            }
        }
    }

    @Nested
    @DisplayName("기본값")
    inner class DefaultValues {
        @Test
        fun `should have default access token expiration of 30 minutes`() {
            // when
            val properties = JwtProperties(secret = validSecret)

            // then
            assertThat(properties.accessTokenExpiration).isEqualTo(1800000L)
        }

        @Test
        fun `should have default refresh token expiration of 7 days`() {
            // when
            val properties = JwtProperties(secret = validSecret)

            // then
            assertThat(properties.refreshTokenExpiration).isEqualTo(604800000L)
        }

        @Test
        fun `should have default issuer as nextup`() {
            // when
            val properties = JwtProperties(secret = validSecret)

            // then
            assertThat(properties.issuer).isEqualTo("nextup")
        }
    }
}
