package com.nextup.infrastructure.security.jwt

import com.nextup.common.exception.InvalidTokenException
import com.nextup.common.exception.TokenExpiredException
import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT 토큰 생성 및 검증을 담당하는 Provider
 */
@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private val secretKey: SecretKey by lazy {
        val keyBytes = Decoders.BASE64.decode(jwtProperties.secret)
        Keys.hmacShaKeyFor(keyBytes)
    }

    /**
     * Access Token을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param roles 사용자 권한 목록
     * @return 생성된 Access Token
     */
    fun createAccessToken(
        userId: Long,
        email: String,
        roles: Set<String>,
    ): String {
        val now = Instant.now()
        val expiration = now.plusMillis(jwtProperties.accessTokenExpiration)

        return Jwts
            .builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("roles", roles.toList())
            .claim("type", "access")
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    /**
     * Refresh Token을 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 Refresh Token
     */
    fun createRefreshToken(userId: Long): String {
        val now = Instant.now()
        val expiration = now.plusMillis(jwtProperties.refreshTokenExpiration)

        return Jwts
            .builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    /**
     * 토큰에서 사용자 ID를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     * @throws InvalidTokenException 토큰이 유효하지 않은 경우
     * @throws TokenExpiredException 토큰이 만료된 경우
     */
    fun getUserId(token: String): Long = getClaims(token).subject.toLong()

    /**
     * 토큰에서 이메일을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    fun getEmail(token: String): String = getClaims(token).get("email", String::class.java)

    /**
     * 토큰에서 권한 목록을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 권한 목록
     */
    @Suppress("UNCHECKED_CAST")
    fun getRoles(token: String): Set<String> {
        val roles = getClaims(token).get("roles", List::class.java) as? List<String>
        return roles?.toSet() ?: emptySet()
    }

    /**
     * 토큰이 Access Token인지 확인합니다.
     *
     * @param token JWT 토큰
     * @return Access Token이면 true
     */
    fun isAccessToken(token: String): Boolean = getClaims(token).get("type", String::class.java) == "access"

    /**
     * 토큰이 Refresh Token인지 확인합니다.
     *
     * @param token JWT 토큰
     * @return Refresh Token이면 true
     */
    fun isRefreshToken(token: String): Boolean = getClaims(token).get("type", String::class.java) == "refresh"

    /**
     * 토큰의 유효성을 검증합니다.
     *
     * @param token JWT 토큰
     * @return 유효하면 true
     */
    fun validateToken(token: String): Boolean =
        try {
            getClaims(token)
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Refresh Token의 만료 시간을 반환합니다.
     *
     * @return Refresh Token 만료 시간 (Instant)
     */
    fun getRefreshTokenExpiration(): Instant = Instant.now().plusMillis(jwtProperties.refreshTokenExpiration)

    private fun getClaims(token: String): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw TokenExpiredException("Token has expired")
        } catch (e: JwtException) {
            throw InvalidTokenException("Invalid token: ${e.message}")
        } catch (e: IllegalArgumentException) {
            throw InvalidTokenException("Token is empty or malformed")
        }
}
