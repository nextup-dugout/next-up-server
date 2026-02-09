package com.nextup.infrastructure.repository.notification

import com.nextup.core.domain.notification.NotificationPreference
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.port.repository.NotificationPreferenceRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceRepository :
    JpaRepository<NotificationPreference, Long>,
    NotificationPreferenceRepositoryPort {
    override fun findByUserId(userId: Long): List<NotificationPreference>

    override fun findByUserIdAndType(
        userId: Long,
        type: NotificationType,
    ): NotificationPreference?
}
