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

    /**
     * 사용자의 미읽은 알림 개수를 조회합니다.
     */
    fun countUnreadByUserId(userId: Long): Long
}
