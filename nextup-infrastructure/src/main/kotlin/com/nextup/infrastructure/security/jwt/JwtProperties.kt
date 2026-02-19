package com.nextup.infrastructure.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Base64

/**
 * JWT 관련 설정을 관리하는 Properties 클래스
 */
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    /**
     * JWT 서명에 사용할 비밀 키 (Base64 인코딩)
     * 환경변수 JWT_SECRET 필수 설정
     */
    val secret: String,
    /**
     * Access Token 만료 시간 (밀리초)
     * 기본값: 30분 (1800000ms)
     */
    val accessTokenExpiration: Long = 1800000L,
    /**
     * Refresh Token 만료 시간 (밀리초)
     * 기본값: 7일 (604800000ms)
     */
    val refreshTokenExpiration: Long = 604800000L,
    /**
     * JWT 발급자
     */
    val issuer: String = "nextup",
) {
    companion object {
        private const val MIN_SECRET_BYTES = 32
    }

    init {
        require(secret.isNotBlank()) {
            "JWT_SECRET 환경변수가 설정되지 않았습니다. 운영 환경에서는 반드시 설정해야 합니다."
        }

        val decodedLength =
            try {
                Base64.getDecoder().decode(secret).size
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "JWT_SECRET이 유효한 Base64 형식이 아닙니다.",
                )
            }

        require(decodedLength >= MIN_SECRET_BYTES) {
            "JWT_SECRET은 최소 256비트(32바이트) 이상이어야 합니다. 현재: ${decodedLength * 8}비트"
        }
    }
}
