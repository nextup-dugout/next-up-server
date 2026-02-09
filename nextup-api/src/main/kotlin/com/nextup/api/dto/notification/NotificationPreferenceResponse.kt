package com.nextup.api.dto.notification

import com.nextup.core.domain.notification.NotificationPreference
import com.nextup.core.domain.notification.NotificationType
import java.time.Instant

/**
 * 알림 설정 응답 DTO
 */
data class NotificationPreferenceResponse(
    val id: Long,
    val userId: Long,
    val type: NotificationType,
    val enabled: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(preference: NotificationPreference): NotificationPreferenceResponse =
            NotificationPreferenceResponse(
                id = preference.id,
                userId = preference.userId,
                type = preference.type,
                enabled = preference.enabled,
                createdAt = preference.createdAt,
            )
    }
}
