package com.nextup.core.domain.notification

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*

/**
 * 디바이스 토큰 엔티티
 *
 * FCM 푸시 알림을 위한 디바이스 토큰을 관리합니다.
 */
@Entity
@Table(
    name = "device_tokens",
    indexes = [
        Index(name = "idx_device_tokens_user_id", columnList = "user_id"),
        Index(name = "idx_device_tokens_token", columnList = "token", unique = true),
        Index(name = "idx_device_tokens_user_platform", columnList = "user_id, platform"),
    ],
)
class DeviceToken private constructor(
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false, length = 500)
    val token: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val platform: DevicePlatform,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        /**
         * 디바이스 토큰을 생성합니다.
         */
        fun create(
            userId: Long,
            token: String,
            platform: DevicePlatform,
        ): DeviceToken {
            require(userId > 0) { "사용자 ID는 필수입니다" }
            require(token.isNotBlank()) { "토큰은 필수입니다" }

            return DeviceToken(
                userId = userId,
                token = token,
                platform = platform,
            )
        }
    }
}
