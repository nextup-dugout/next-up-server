package com.nextup.core.service.notification.dto

import com.nextup.core.domain.notification.NotificationType

/**
 * 알림 전송 요청 DTO
 */
data class SendNotificationRequest(
    val userId: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: String? = null,
)
