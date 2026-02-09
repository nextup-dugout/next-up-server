package com.nextup.infrastructure.repository.notification

import com.nextup.core.domain.notification.Notification
import com.nextup.core.port.repository.NotificationRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository :
    JpaRepository<Notification, Long>,
    NotificationRepositoryPort {
    override fun findByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<Notification>

    override fun findByUserId(userId: Long): List<Notification>
}
