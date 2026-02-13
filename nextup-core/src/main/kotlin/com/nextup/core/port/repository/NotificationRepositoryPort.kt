package com.nextup.core.port.repository

import com.nextup.core.domain.notification.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface NotificationRepositoryPort {
    fun save(notification: Notification): Notification

    fun findByIdOrNull(id: Long): Notification?

    fun findByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<Notification>

    fun findByUserId(userId: Long): List<Notification>
}
