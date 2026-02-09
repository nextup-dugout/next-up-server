package com.nextup.api.dto.notification

import com.nextup.core.domain.notification.Notification
import com.nextup.core.domain.notification.NotificationType
import java.time.Instant
import java.time.LocalDateTime

/**
 * 알림 응답 DTO
 */
data class NotificationResponse(
    val id: Long,
    val userId: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: String?,
    val readAt: LocalDateTime?,
    val sentAt: LocalDateTime?,
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                id = notification.id,
                userId = notification.userId,
                type = notification.type,
                title = notification.title,
                body = notification.body,
                data = notification.data,
                readAt = notification.readAt,
                sentAt = notification.sentAt,
                createdAt = notification.createdAt,
            )
    }
}
