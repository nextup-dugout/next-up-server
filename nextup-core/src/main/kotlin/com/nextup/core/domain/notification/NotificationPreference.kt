package com.nextup.core.domain.notification

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*

/**
 * 알림 설정 엔티티
 *
 * 사용자별 알림 타입에 대한 수신 설정을 관리합니다.
 */
@Entity
@Table(
    name = "notification_preferences",
    indexes = [
        Index(name = "idx_notification_preferences_user_id", columnList = "user_id"),
        Index(
            name = "idx_notification_preferences_user_type",
            columnList = "user_id, type",
            unique = true,
        ),
    ],
)
class NotificationPreference private constructor(
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: NotificationType,
    @Column(nullable = false)
    var enabled: Boolean = true,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 알림을 활성화합니다.
     */
    fun enable() {
        enabled = true
    }

    /**
     * 알림을 비활성화합니다.
     */
    fun disable() {
        enabled = false
    }

    /**
     * 활성화 상태를 토글합니다.
     */
    fun toggle() {
        enabled = !enabled
    }

    companion object {
        /**
         * 알림 설정을 생성합니다.
         */
        fun create(
            userId: Long,
            type: NotificationType,
            enabled: Boolean = true,
        ): NotificationPreference {
            require(userId > 0) { "사용자 ID는 필수입니다" }

            return NotificationPreference(
                userId = userId,
                type = type,
                enabled = enabled,
            )
        }
    }
}
