package com.nextup.core.service.notification

import com.nextup.common.exception.DeviceTokenNotFoundException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.NotificationNotFoundException
import com.nextup.core.domain.notification.DeviceToken
import com.nextup.core.domain.notification.Notification
import com.nextup.core.domain.notification.NotificationPreference
import com.nextup.core.port.repository.DeviceTokenRepositoryPort
import com.nextup.core.port.repository.NotificationPreferenceRepositoryPort
import com.nextup.core.port.repository.NotificationRepositoryPort
import com.nextup.core.service.notification.dto.RegisterDeviceRequest
import com.nextup.core.service.notification.dto.SendNotificationRequest
import com.nextup.core.service.notification.dto.UpdatePreferenceRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 알림 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepositoryPort,
    private val deviceTokenRepository: DeviceTokenRepositoryPort,
    private val preferenceRepository: NotificationPreferenceRepositoryPort,
) {
    /**
     * 알림을 전송합니다.
     */
    @Transactional
    fun sendNotification(request: SendNotificationRequest): Notification {
        val notification =
            Notification.create(
                userId = request.userId,
                type = request.type,
                title = request.title,
                body = request.body,
                data = request.data,
            )

        notification.markAsSent()
        return notificationRepository.save(notification)
    }

    /**
     * 사용자의 알림 목록을 조회합니다 (페이징).
     */
    fun getUserNotifications(
        userId: Long,
        pageable: Pageable,
    ): Page<Notification> = notificationRepository.findByUserId(userId, pageable)

    /**
     * 사용자의 알림 목록을 조회합니다.
     */
    fun getUserNotifications(userId: Long): List<Notification> = notificationRepository.findByUserId(userId)

    /**
     * 알림을 읽음 처리합니다.
     * 소유권 검증: 인증된 사용자만 자신의 알림을 읽음 처리할 수 있습니다.
     */
    @Transactional
    fun markAsRead(
        notificationId: Long,
        authenticatedUserId: Long,
    ): Notification {
        val notification = getById(notificationId)
        verifyNotificationOwnership(notification, authenticatedUserId)
        notification.markAsRead()
        return notification
    }

    /**
     * 알림을 읽음 처리합니다 (내부 호출용).
     */
    @Transactional
    fun markAsRead(notificationId: Long): Notification {
        val notification = getById(notificationId)
        notification.markAsRead()
        return notification
    }

    /**
     * ID로 알림을 조회합니다.
     */
    fun getById(id: Long): Notification =
        notificationRepository.findByIdOrNull(id)
            ?: throw NotificationNotFoundException(id)

    /**
     * 디바이스 토큰을 등록합니다.
     */
    @Transactional
    fun registerDevice(request: RegisterDeviceRequest): DeviceToken {
        // 기존 토큰이 있으면 삭제 (중복 방지)
        deviceTokenRepository.findByToken(request.token)?.let {
            deviceTokenRepository.deleteByToken(request.token)
        }

        val deviceToken =
            DeviceToken.create(
                userId = request.userId,
                token = request.token,
                platform = request.platform,
            )

        return deviceTokenRepository.save(deviceToken)
    }

    /**
     * 디바이스 토큰을 삭제합니다.
     * 소유권 검증: 인증된 사용자만 자신의 디바이스 토큰을 삭제할 수 있습니다.
     */
    @Transactional
    fun removeDevice(
        tokenId: Long,
        authenticatedUserId: Long,
    ) {
        val deviceToken =
            deviceTokenRepository.findByIdOrNull(tokenId)
                ?: throw DeviceTokenNotFoundException(tokenId)

        verifyDeviceTokenOwnership(deviceToken, authenticatedUserId)
        deviceTokenRepository.deleteByToken(deviceToken.token)
    }

    /**
     * 디바이스 토큰을 삭제합니다 (내부 호출용).
     */
    @Transactional
    fun removeDevice(tokenId: Long) {
        val deviceToken =
            deviceTokenRepository.findByIdOrNull(tokenId)
                ?: throw DeviceTokenNotFoundException(tokenId)

        deviceTokenRepository.deleteByToken(deviceToken.token)
    }

    /**
     * 사용자의 디바이스 토큰 목록을 조회합니다.
     */
    fun getUserDevices(userId: Long): List<DeviceToken> = deviceTokenRepository.findByUserId(userId)

    /**
     * 알림 설정을 업데이트합니다.
     */
    @Transactional
    fun updatePreference(request: UpdatePreferenceRequest): NotificationPreference {
        val preference =
            preferenceRepository.findByUserIdAndType(request.userId, request.type)
                ?: NotificationPreference.create(
                    userId = request.userId,
                    type = request.type,
                    enabled = request.enabled,
                )

        if (request.enabled) {
            preference.enable()
        } else {
            preference.disable()
        }

        return preferenceRepository.save(preference)
    }

    /**
     * 사용자의 알림 설정 목록을 조회합니다.
     */
    fun getUserPreferences(userId: Long): List<NotificationPreference> = preferenceRepository.findByUserId(userId)

    private fun verifyNotificationOwnership(
        notification: Notification,
        authenticatedUserId: Long,
    ) {
        if (notification.userId != authenticatedUserId) {
            throw ForbiddenException(
                "NOTIFICATION_ACCESS_DENIED",
                "해당 알림에 접근 권한이 없습니다",
            )
        }
    }

    private fun verifyDeviceTokenOwnership(
        deviceToken: DeviceToken,
        authenticatedUserId: Long,
    ) {
        if (deviceToken.userId != authenticatedUserId) {
            throw ForbiddenException(
                "DEVICE_TOKEN_ACCESS_DENIED",
                "해당 디바이스 토큰에 접근 권한이 없습니다",
            )
        }
    }
}
