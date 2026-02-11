package com.nextup.core.domain.notification

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 알림 엔티티
 *
 * 사용자에게 전송되는 푸시 알림을 관리합니다.
 */
@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_user_id", columnList = "user_id"),
        Index(name = "idx_notifications_type", columnList = "type"),
        Index(name = "idx_notifications_user_type", columnList = "user_id, type"),
        Index(name = "idx_notifications_sent_at", columnList = "sent_at"),
    ],
)
class Notification private constructor(
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: NotificationType,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val body: String,
    @Column(columnDefinition = "TEXT")
    val data: String? = null,
    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,
    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 알림을 읽음 처리합니다.
     */
    fun markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now()
        }
    }

    /**
     * 알림 전송을 기록합니다.
     */
    fun markAsSent() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now()
        }
    }

    /**
     * 알림이 읽혔는지 확인합니다.
     */
    fun isRead(): Boolean = readAt != null

    /**
     * 알림이 전송되었는지 확인합니다.
     */
    fun isSent(): Boolean = sentAt != null

    companion object {
        /**
         * 알림을 생성합니다.
         */
        fun create(
            userId: Long,
            type: NotificationType,
            title: String,
            body: String,
            data: String? = null,
        ): Notification {
            require(userId > 0) { "사용자 ID는 필수입니다" }
            require(title.isNotBlank()) { "제목은 필수입니다" }
            require(body.isNotBlank()) { "내용은 필수입니다" }

            return Notification(
                userId = userId,
                type = type,
                title = title,
                body = body,
                data = data,
            )
        }
    }
}
