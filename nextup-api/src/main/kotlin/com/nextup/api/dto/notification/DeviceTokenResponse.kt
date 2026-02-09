package com.nextup.api.dto.notification

import com.nextup.core.domain.notification.DevicePlatform
import com.nextup.core.domain.notification.DeviceToken
import java.time.Instant

/**
 * 디바이스 토큰 응답 DTO
 */
data class DeviceTokenResponse(
    val id: Long,
    val userId: Long,
    val token: String,
    val platform: DevicePlatform,
    val createdAt: Instant,
) {
    companion object {
        fun from(deviceToken: DeviceToken): DeviceTokenResponse =
            DeviceTokenResponse(
                id = deviceToken.id,
                userId = deviceToken.userId,
                token = deviceToken.token,
                platform = deviceToken.platform,
                createdAt = deviceToken.createdAt,
            )
    }
}
