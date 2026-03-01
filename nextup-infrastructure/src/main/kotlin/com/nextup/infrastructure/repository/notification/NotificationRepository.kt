package com.nextup.infrastructure.repository.notification

import com.nextup.core.domain.notification.Notification
import com.nextup.core.port.repository.NotificationRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository :
    JpaRepository<Notification, Long>,
    NotificationRepositoryPort {
    override fun findByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<Notification>

    override fun findByUserId(userId: Long): List<Notification>

    @Query(
        "SELECT COUNT(n) FROM Notification n " +
            "WHERE n.userId = :userId AND n.readAt IS NULL",
    )
    fun countUnreadNotificationsByUserId(
        @Param("userId") userId: Long,
    ): Long

    override fun countUnreadByUserId(userId: Long): Long =
        countUnreadNotificationsByUserId(userId)
}
