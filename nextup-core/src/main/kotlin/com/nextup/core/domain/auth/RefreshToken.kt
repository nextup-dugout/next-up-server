package com.nextup.core.domain.auth

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * Refresh Token 엔티티
 *
 * JWT Access Token 갱신을 위한 Refresh Token을 관리합니다.
 * 사용자당 여러 개의 Refresh Token을 가질 수 있습니다 (멀티 디바이스 지원).
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_token_user", columnList = "user_id"),
        Index(name = "idx_refresh_token_token", columnList = "token", unique = true),
        Index(name = "idx_refresh_token_expires", columnList = "expires_at"),
    ],
)
class RefreshToken private constructor(
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false, unique = true, length = 500)
    val token: String,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "is_revoked", nullable = false)
    var isRevoked: Boolean = false,
    @Column(name = "device_info", length = 255)
    val deviceInfo: String? = null,
    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 토큰이 만료되었는지 확인합니다.
     */
    val isExpired: Boolean
        get() = Instant.now().isAfter(expiresAt)

    /**
     * 토큰이 유효한지 확인합니다 (만료되지 않고 폐기되지 않음).
     */
    val isValid: Boolean
        get() = !isExpired && !isRevoked

    /**
     * 토큰을 폐기합니다.
     *
     * 이미 폐기된 토큰에 대해서는 아무 작업도 수행하지 않습니다.
     */
    fun revoke() {
        if (!isRevoked) {
            isRevoked = true
        }
    }

    /**
     * 토큰이 유효한지 검증합니다.
     *
     * @throws IllegalStateException 토큰이 만료되었거나 폐기된 경우
     */
    fun validate() {
        require(!isExpired) { "Refresh token has expired" }
        require(!isRevoked) { "Refresh token has been revoked" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RefreshToken) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "RefreshToken(id=$id, userId=$userId, isRevoked=$isRevoked, expiresAt=$expiresAt)"

    companion object {
        /**
         * RefreshToken을 생성합니다.
         *
         * @param userId 사용자 ID
         * @param token 토큰 문자열
         * @param expiresAt 만료 시간
         * @param deviceInfo 디바이스 정보 (선택)
         * @param ipAddress 접속 IP 주소 (선택)
         * @return 생성된 RefreshToken
         * @throws IllegalArgumentException 토큰이 비어있거나 만료 시간이 현재 시간 이전인 경우
         */
        fun create(
            userId: Long,
            token: String,
            expiresAt: Instant,
            deviceInfo: String? = null,
            ipAddress: String? = null,
        ): RefreshToken {
            require(token.isNotBlank()) { "Token cannot be blank" }
            require(expiresAt.isAfter(Instant.now())) {
                "Expiration time must be in the future"
            }

            return RefreshToken(
                userId = userId,
                token = token,
                expiresAt = expiresAt,
                deviceInfo = deviceInfo,
                ipAddress = ipAddress,
            )
        }
    }
}
