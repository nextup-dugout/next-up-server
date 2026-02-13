package com.nextup.core.service.notification.dto

import com.nextup.core.domain.notification.NotificationType

/**
 * 알림 설정 수정 요청 DTO
 */
data class UpdatePreferenceRequest(
    val userId: Long,
    val type: NotificationType,
    val enabled: Boolean,
)
