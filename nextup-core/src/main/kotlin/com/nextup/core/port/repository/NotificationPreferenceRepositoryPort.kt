package com.nextup.core.port.repository

import com.nextup.core.domain.notification.NotificationPreference
import com.nextup.core.domain.notification.NotificationType

interface NotificationPreferenceRepositoryPort {
    fun save(preference: NotificationPreference): NotificationPreference

    fun findByUserId(userId: Long): List<NotificationPreference>

    fun findByUserIdAndType(
        userId: Long,
        type: NotificationType,
    ): NotificationPreference?
}
