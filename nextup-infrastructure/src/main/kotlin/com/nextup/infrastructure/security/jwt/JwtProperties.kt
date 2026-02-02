package com.nextup.infrastructure.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT 관련 설정을 관리하는 Properties 클래스
 */
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    /**
     * JWT 서명에 사용할 비밀 키 (Base64 인코딩)
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
    val issuer: String = "nextup"
)
