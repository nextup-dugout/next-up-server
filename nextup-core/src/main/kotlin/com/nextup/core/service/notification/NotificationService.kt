package com.nextup.core.service.notification

import com.nextup.common.exception.DeviceTokenNotFoundException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.NotificationNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.notification.DeviceToken
import com.nextup.core.domain.notification.Notification
import com.nextup.core.domain.notification.NotificationPreference
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.port.repository.DeviceTokenRepositoryPort
import com.nextup.core.port.repository.NotificationPreferenceRepositoryPort
import com.nextup.core.port.repository.NotificationRepositoryPort
import com.nextup.core.port.service.PushNotificationPort
import com.nextup.core.service.notification.dto.RegisterDeviceRequest
import com.nextup.core.service.notification.dto.SendNotificationRequest
import com.nextup.core.service.notification.dto.UpdatePreferenceRequest
import org.slf4j.LoggerFactory
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
    private val pushNotificationPort: PushNotificationPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 알림을 전송합니다.
     *
     * 사용자의 알림 설정(Preference)을 확인한 후,
     * DB에 알림을 저장하고 사용자의 등록된 디바이스로 푸시 알림을 발송합니다.
     * 사용자가 해당 알림 타입을 비활성화한 경우 발송을 스킵합니다.
     * 설정이 없는 경우 기본적으로 발송합니다 (opt-out 모델).
     *
     * @return 알림이 발송된 경우 Notification, 스킵된 경우 null
     */
    @Transactional
    fun sendNotification(request: SendNotificationRequest): Notification? {
        if (!isNotificationEnabled(request.userId, request.type)) {
            log.debug(
                "알림 설정에 의해 발송 스킵: userId={}, type={}",
                request.userId,
                request.type,
            )
            return null
        }

        val notification =
            Notification.create(
                userId = request.userId,
                type = request.type,
                title = request.title,
                body = request.body,
                data = request.data,
            )

        notification.markAsSent()
        val saved = notificationRepository.save(notification)

        sendPushToUser(request)

        return saved
    }

    /**
     * 알림을 배치로 전송합니다.
     *
     * 대량 알림 발송 시 N+1 쿼리를 방지하기 위해 saveAll()로 배치 저장합니다.
     * 각 사용자에게 푸시 알림도 발송합니다.
     */
    @Transactional
    fun sendBatchNotifications(requests: List<SendNotificationRequest>): List<Notification> {
        if (requests.isEmpty()) return emptyList()

        val notifications =
            requests.map { request ->
                Notification.create(
                    userId = request.userId,
                    type = request.type,
                    title = request.title,
                    body = request.body,
                    data = request.data,
                ).apply { markAsSent() }
            }

        val saved = notifications.map { notificationRepository.save(it) }

        requests.forEach { request -> sendPushToUser(request) }

        return saved
    }

    /**
     * 사용자의 알림 설정을 확인합니다.
     *
     * 설정이 존재하지 않으면 기본적으로 활성화(opt-out 모델)입니다.
     */
    private fun isNotificationEnabled(
        userId: Long,
        type: NotificationType,
    ): Boolean {
        val preference =
            preferenceRepository.findByUserIdAndType(userId, type)
        return preference?.enabled ?: true
    }

    private fun sendPushToUser(request: SendNotificationRequest) {
        try {
            val tokens = deviceTokenRepository.findByUserId(request.userId)
            if (tokens.isEmpty()) return

            val dataMap =
                request.data?.let { mapOf("data" to it) }

            val tokenStrings = tokens.map { it.token }
            val successCount =
                pushNotificationPort.sendBatch(
                    tokens = tokenStrings,
                    title = request.title,
                    body = request.body,
                    data = dataMap,
                )
            log.debug(
                "푸시 발송 완료: userId={}, total={}, success={}",
                request.userId,
                tokenStrings.size,
                successCount,
            )
        } catch (e: Exception) {
            log.warn("푸시 발송 실패 (알림 저장은 완료): userId={}, error={}", request.userId, e.message)
        }
    }

    /**
     * 사용자의 알림 목록을 조회합니다 (페이징).
     */
    fun getUserNotifications(
        userId: Long,
        pageCommand: PageCommand,
    ): PageResult<Notification> = notificationRepository.findByUserId(userId, pageCommand)

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

    /**
     * 사용자의 미읽은 알림 개수를 조회합니다.
     */
    fun getUnreadCount(userId: Long): Long = notificationRepository.countUnreadByUserId(userId)

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
