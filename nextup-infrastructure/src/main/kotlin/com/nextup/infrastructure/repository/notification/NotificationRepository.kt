package com.nextup.infrastructure.repository.notification

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.notification.Notification
import com.nextup.core.port.repository.NotificationRepositoryPort
import com.nextup.infrastructure.common.toPageResult
import com.nextup.infrastructure.common.toPageable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository :
    JpaRepository<Notification, Long>,
    NotificationRepositoryPort {
    override fun findByIdOrNull(id: Long): Notification? = findById(id).orElse(null)

    fun findByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<Notification>

    override fun findByUserId(userId: Long): List<Notification>

    override fun findByUserId(
        userId: Long,
        pageCommand: PageCommand,
    ): PageResult<Notification> = findByUserId(userId, pageCommand.toPageable()).toPageResult()

    @Query(
        "SELECT COUNT(n) FROM Notification n " +
            "WHERE n.userId = :userId AND n.readAt IS NULL",
    )
    fun countUnreadNotificationsByUserId(
        @Param("userId") userId: Long,
    ): Long

    override fun countUnreadByUserId(userId: Long): Long = countUnreadNotificationsByUserId(userId)

    @Modifying
    @Query(
        "UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP " +
            "WHERE n.userId = :userId AND n.readAt IS NULL",
    )
    fun markAllAsReadForUser(
        @Param("userId") userId: Long,
    )

    override fun markAllAsReadByUserId(userId: Long) = markAllAsReadForUser(userId)
}
