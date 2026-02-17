package com.nextup.core.service.notification

import com.nextup.common.exception.DeviceTokenNotFoundException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.NotificationNotFoundException
import com.nextup.core.domain.notification.DevicePlatform
import com.nextup.core.domain.notification.DeviceToken
import com.nextup.core.domain.notification.Notification
import com.nextup.core.domain.notification.NotificationPreference
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.port.repository.DeviceTokenRepositoryPort
import com.nextup.core.port.repository.NotificationPreferenceRepositoryPort
import com.nextup.core.port.repository.NotificationRepositoryPort
import com.nextup.core.service.notification.dto.RegisterDeviceRequest
import com.nextup.core.service.notification.dto.SendNotificationRequest
import com.nextup.core.service.notification.dto.UpdatePreferenceRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class NotificationServiceTest {
    private lateinit var notificationRepository: NotificationRepositoryPort
    private lateinit var deviceTokenRepository: DeviceTokenRepositoryPort
    private lateinit var preferenceRepository: NotificationPreferenceRepositoryPort
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationRepository = mockk()
        deviceTokenRepository = mockk()
        preferenceRepository = mockk()
        notificationService =
            NotificationService(
                notificationRepository,
                deviceTokenRepository,
                preferenceRepository,
            )
    }

    // ========== sendNotification Tests ==========

    @Test
    fun `알림을 전송할 수 있다`() {
        // given
        val request =
            SendNotificationRequest(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "경기가 곧 시작됩니다",
                data = """{"gameId": 123}""",
            )

        every { notificationRepository.save(any()) } answers { firstArg() }

        // when
        val result = notificationService.sendNotification(request)

        // then
        assertThat(result.userId).isEqualTo(request.userId)
        assertThat(result.type).isEqualTo(request.type)
        assertThat(result.title).isEqualTo(request.title)
        assertThat(result.body).isEqualTo(request.body)
        assertThat(result.data).isEqualTo(request.data)
        assertThat(result.isSent()).isTrue()

        verify(exactly = 1) { notificationRepository.save(any()) }
    }

    @Test
    fun `data 없이 알림을 전송할 수 있다`() {
        // given
        val request =
            SendNotificationRequest(
                userId = 1L,
                type = NotificationType.TEAM_NOTICE,
                title = "팀 공지",
                body = "중요 공지사항입니다",
                data = null,
            )

        every { notificationRepository.save(any()) } answers { firstArg() }

        // when
        val result = notificationService.sendNotification(request)

        // then
        assertThat(result.data).isNull()
        assertThat(result.isSent()).isTrue()
    }

    // ========== getUserNotifications Tests (Paging) ==========

    @Test
    fun `사용자의 알림 목록을 페이징으로 조회할 수 있다`() {
        // given
        val userId = 1L
        val pageable = PageRequest.of(0, 10)

        val notifications =
            listOf(
                Notification.create(
                    userId = userId,
                    type = NotificationType.GAME_START,
                    title = "알림 1",
                    body = "내용 1",
                ),
                Notification.create(
                    userId = userId,
                    type = NotificationType.TEAM_NOTICE,
                    title = "알림 2",
                    body = "내용 2",
                ),
            )

        val page = PageImpl(notifications, pageable, notifications.size.toLong())

        every { notificationRepository.findByUserId(userId, pageable) } returns page

        // when
        val result = notificationService.getUserNotifications(userId, pageable)

        // then
        assertThat(result.content).hasSize(2)
        assertThat(result.content[0].title).isEqualTo("알림 1")
        assertThat(result.content[1].title).isEqualTo("알림 2")
        assertThat(result.totalElements).isEqualTo(2)

        verify(exactly = 1) { notificationRepository.findByUserId(userId, pageable) }
    }

    @Test
    fun `알림이 없는 사용자의 페이징 조회는 빈 페이지를 반환한다`() {
        // given
        val userId = 1L
        val pageable = PageRequest.of(0, 10)
        val emptyPage = PageImpl<Notification>(emptyList(), pageable, 0)

        every { notificationRepository.findByUserId(userId, pageable) } returns emptyPage

        // when
        val result = notificationService.getUserNotifications(userId, pageable)

        // then
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    // ========== getUserNotifications Tests (List) ==========

    @Test
    fun `사용자의 알림 목록을 전체 조회할 수 있다`() {
        // given
        val userId = 1L

        val notifications =
            listOf(
                Notification.create(
                    userId = userId,
                    type = NotificationType.GAME_START,
                    title = "알림 1",
                    body = "내용 1",
                ),
                Notification.create(
                    userId = userId,
                    type = NotificationType.ATTENDANCE_NUDGE,
                    title = "알림 2",
                    body = "내용 2",
                ),
            )

        every { notificationRepository.findByUserId(userId) } returns notifications

        // when
        val result = notificationService.getUserNotifications(userId)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("알림 1")
        assertThat(result[1].title).isEqualTo("알림 2")

        verify(exactly = 1) { notificationRepository.findByUserId(userId) }
    }

    @Test
    fun `알림이 없는 사용자의 전체 조회는 빈 리스트를 반환한다`() {
        // given
        val userId = 1L

        every { notificationRepository.findByUserId(userId) } returns emptyList()

        // when
        val result = notificationService.getUserNotifications(userId)

        // then
        assertThat(result).isEmpty()
    }

    // ========== markAsRead Tests ==========

    @Test
    fun `알림을 읽음 처리할 수 있다`() {
        // given
        val notificationId = 1L
        val notification =
            Notification.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "알림",
                body = "내용",
            )

        every { notificationRepository.findByIdOrNull(notificationId) } returns notification

        // when
        val result = notificationService.markAsRead(notificationId)

        // then
        assertThat(result.isRead()).isTrue()
        assertThat(result.readAt).isNotNull()

        verify(exactly = 1) { notificationRepository.findByIdOrNull(notificationId) }
    }

    @Test
    fun `존재하지 않는 알림을 읽음 처리하면 예외가 발생한다`() {
        // given
        val notificationId = 999L

        every { notificationRepository.findByIdOrNull(notificationId) } returns null

        // when & then
        val exception =
            assertThrows<NotificationNotFoundException> {
                notificationService.markAsRead(notificationId)
            }

        assertThat(exception.message).contains("999")
    }

    // ========== markAsRead with ownership verification Tests ==========

    @Test
    fun `인증된 사용자가 자신의 알림을 읽음 처리할 수 있다`() {
        // given
        val notificationId = 1L
        val authenticatedUserId = 1L
        val notification =
            Notification.create(
                userId = authenticatedUserId,
                type = NotificationType.GAME_START,
                title = "알림",
                body = "내용",
            )

        every { notificationRepository.findByIdOrNull(notificationId) } returns notification

        // when
        val result = notificationService.markAsRead(notificationId, authenticatedUserId)

        // then
        assertThat(result.isRead()).isTrue()
        assertThat(result.readAt).isNotNull()

        verify(exactly = 1) { notificationRepository.findByIdOrNull(notificationId) }
    }

    @Test
    fun `다른 사용자의 알림을 읽음 처리하면 ForbiddenException이 발생한다`() {
        // given
        val notificationId = 1L
        val ownerUserId = 1L
        val otherUserId = 999L
        val notification =
            Notification.create(
                userId = ownerUserId,
                type = NotificationType.GAME_START,
                title = "알림",
                body = "내용",
            )

        every { notificationRepository.findByIdOrNull(notificationId) } returns notification

        // when & then
        val exception =
            assertThrows<ForbiddenException> {
                notificationService.markAsRead(notificationId, otherUserId)
            }

        assertThat(exception.code).isEqualTo("NOTIFICATION_ACCESS_DENIED")
    }

    @Test
    fun `존재하지 않는 알림을 소유권 검증으로 읽음 처리하면 NotificationNotFoundException이 발생한다`() {
        // given
        val notificationId = 999L
        val authenticatedUserId = 1L

        every { notificationRepository.findByIdOrNull(notificationId) } returns null

        // when & then
        assertThrows<NotificationNotFoundException> {
            notificationService.markAsRead(notificationId, authenticatedUserId)
        }
    }

    // ========== getById Tests ==========

    @Test
    fun `ID로 알림을 조회할 수 있다`() {
        // given
        val notificationId = 1L
        val notification =
            Notification.create(
                userId = 1L,
                type = NotificationType.RECORD_ALERT,
                title = "기록 알림",
                body = "기록이 업데이트되었습니다",
            )

        every { notificationRepository.findByIdOrNull(notificationId) } returns notification

        // when
        val result = notificationService.getById(notificationId)

        // then
        assertThat(result.title).isEqualTo("기록 알림")
        assertThat(result.type).isEqualTo(NotificationType.RECORD_ALERT)

        verify(exactly = 1) { notificationRepository.findByIdOrNull(notificationId) }
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 예외가 발생한다`() {
        // given
        val notificationId = 999L

        every { notificationRepository.findByIdOrNull(notificationId) } returns null

        // when & then
        val exception =
            assertThrows<NotificationNotFoundException> {
                notificationService.getById(notificationId)
            }

        assertThat(exception.message).contains("999")
    }

    // ========== registerDevice Tests ==========

    @Test
    fun `디바이스 토큰을 등록할 수 있다`() {
        // given
        val request =
            RegisterDeviceRequest(
                userId = 1L,
                token = "fcm-token-12345",
                platform = DevicePlatform.ANDROID,
            )

        every { deviceTokenRepository.findByToken(request.token) } returns null
        every { deviceTokenRepository.save(any()) } answers { firstArg() }

        // when
        val result = notificationService.registerDevice(request)

        // then
        assertThat(result.userId).isEqualTo(request.userId)
        assertThat(result.token).isEqualTo(request.token)
        assertThat(result.platform).isEqualTo(request.platform)

        verify(exactly = 1) { deviceTokenRepository.findByToken(request.token) }
        verify(exactly = 1) { deviceTokenRepository.save(any()) }
        verify(exactly = 0) { deviceTokenRepository.deleteByToken(any()) }
    }

    @Test
    fun `중복된 토큰이 있으면 기존 토큰을 삭제하고 새로 등록한다`() {
        // given
        val request =
            RegisterDeviceRequest(
                userId = 1L,
                token = "fcm-token-12345",
                platform = DevicePlatform.IOS,
            )

        val existingToken =
            DeviceToken.create(
                userId = 2L, // 다른 사용자
                token = request.token,
                platform = DevicePlatform.ANDROID,
            )

        every { deviceTokenRepository.findByToken(request.token) } returns existingToken
        every { deviceTokenRepository.deleteByToken(request.token) } returns Unit
        every { deviceTokenRepository.save(any()) } answers { firstArg() }

        // when
        val result = notificationService.registerDevice(request)

        // then
        assertThat(result.userId).isEqualTo(request.userId)
        assertThat(result.platform).isEqualTo(DevicePlatform.IOS)

        verify(exactly = 1) { deviceTokenRepository.findByToken(request.token) }
        verify(exactly = 1) { deviceTokenRepository.deleteByToken(request.token) }
        verify(exactly = 1) { deviceTokenRepository.save(any()) }
    }

    // ========== removeDevice Tests ==========

    @Test
    fun `디바이스 토큰을 삭제할 수 있다`() {
        // given
        val tokenId = 1L
        val deviceToken =
            DeviceToken.create(
                userId = 1L,
                token = "fcm-token-12345",
                platform = DevicePlatform.WEB,
            )

        every { deviceTokenRepository.findByIdOrNull(tokenId) } returns deviceToken
        every { deviceTokenRepository.deleteByToken(deviceToken.token) } returns Unit

        // when
        notificationService.removeDevice(tokenId)

        // then
        verify(exactly = 1) { deviceTokenRepository.findByIdOrNull(tokenId) }
        verify(exactly = 1) { deviceTokenRepository.deleteByToken(deviceToken.token) }
    }

    @Test
    fun `존재하지 않는 디바이스 토큰을 삭제하면 예외가 발생한다`() {
        // given
        val tokenId = 999L

        every { deviceTokenRepository.findByIdOrNull(tokenId) } returns null

        // when & then
        val exception =
            assertThrows<DeviceTokenNotFoundException> {
                notificationService.removeDevice(tokenId)
            }

        assertThat(exception.message).contains("999")

        verify(exactly = 1) { deviceTokenRepository.findByIdOrNull(tokenId) }
        verify(exactly = 0) { deviceTokenRepository.deleteByToken(any()) }
    }

    // ========== removeDevice with ownership verification Tests ==========

    @Test
    fun `인증된 사용자가 자신의 디바이스 토큰을 삭제할 수 있다`() {
        // given
        val tokenId = 1L
        val authenticatedUserId = 1L
        val deviceToken =
            DeviceToken.create(
                userId = authenticatedUserId,
                token = "fcm-token-12345",
                platform = DevicePlatform.ANDROID,
            )

        every { deviceTokenRepository.findByIdOrNull(tokenId) } returns deviceToken
        every { deviceTokenRepository.deleteByToken(deviceToken.token) } returns Unit

        // when
        notificationService.removeDevice(tokenId, authenticatedUserId)

        // then
        verify(exactly = 1) { deviceTokenRepository.findByIdOrNull(tokenId) }
        verify(exactly = 1) { deviceTokenRepository.deleteByToken(deviceToken.token) }
    }

    @Test
    fun `다른 사용자의 디바이스 토큰을 삭제하면 ForbiddenException이 발생한다`() {
        // given
        val tokenId = 1L
        val ownerUserId = 1L
        val otherUserId = 999L
        val deviceToken =
            DeviceToken.create(
                userId = ownerUserId,
                token = "fcm-token-12345",
                platform = DevicePlatform.ANDROID,
            )

        every { deviceTokenRepository.findByIdOrNull(tokenId) } returns deviceToken

        // when & then
        val exception =
            assertThrows<ForbiddenException> {
                notificationService.removeDevice(tokenId, otherUserId)
            }

        assertThat(exception.code).isEqualTo("DEVICE_TOKEN_ACCESS_DENIED")
        verify(exactly = 0) { deviceTokenRepository.deleteByToken(any()) }
    }

    @Test
    fun `존재하지 않는 디바이스 토큰을 소유권 검증으로 삭제하면 DeviceTokenNotFoundException이 발생한다`() {
        // given
        val tokenId = 999L
        val authenticatedUserId = 1L

        every { deviceTokenRepository.findByIdOrNull(tokenId) } returns null

        // when & then
        assertThrows<DeviceTokenNotFoundException> {
            notificationService.removeDevice(tokenId, authenticatedUserId)
        }

        verify(exactly = 0) { deviceTokenRepository.deleteByToken(any()) }
    }

    // ========== getUserDevices Tests ==========

    @Test
    fun `사용자의 디바이스 토큰 목록을 조회할 수 있다`() {
        // given
        val userId = 1L

        val devices =
            listOf(
                DeviceToken.create(
                    userId = userId,
                    token = "android-token",
                    platform = DevicePlatform.ANDROID,
                ),
                DeviceToken.create(
                    userId = userId,
                    token = "ios-token",
                    platform = DevicePlatform.IOS,
                ),
            )

        every { deviceTokenRepository.findByUserId(userId) } returns devices

        // when
        val result = notificationService.getUserDevices(userId)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].platform).isEqualTo(DevicePlatform.ANDROID)
        assertThat(result[1].platform).isEqualTo(DevicePlatform.IOS)

        verify(exactly = 1) { deviceTokenRepository.findByUserId(userId) }
    }

    @Test
    fun `디바이스가 없는 사용자는 빈 리스트를 반환한다`() {
        // given
        val userId = 1L

        every { deviceTokenRepository.findByUserId(userId) } returns emptyList()

        // when
        val result = notificationService.getUserDevices(userId)

        // then
        assertThat(result).isEmpty()
    }

    // ========== updatePreference Tests ==========

    @Test
    fun `새로운 알림 설정을 생성하여 활성화할 수 있다`() {
        // given
        val request =
            UpdatePreferenceRequest(
                userId = 1L,
                type = NotificationType.GAME_START,
                enabled = true,
            )

        every { preferenceRepository.findByUserIdAndType(request.userId, request.type) } returns null
        every { preferenceRepository.save(any()) } answers { firstArg() }

        // when
        val result = notificationService.updatePreference(request)

        // then
        assertThat(result.userId).isEqualTo(request.userId)
        assertThat(result.type).isEqualTo(request.type)
        assertThat(result.enabled).isTrue()

        verify(exactly = 1) { preferenceRepository.findByUserIdAndType(request.userId, request.type) }
        verify(exactly = 1) { preferenceRepository.save(any()) }
    }

    @Test
    fun `기존 알림 설정을 비활성화할 수 있다`() {
        // given
        val request =
            UpdatePreferenceRequest(
                userId = 1L,
                type = NotificationType.TEAM_NOTICE,
                enabled = false,
            )

        val existingPreference =
            NotificationPreference.create(
                userId = request.userId,
                type = request.type,
                enabled = true,
            )

        every { preferenceRepository.findByUserIdAndType(request.userId, request.type) } returns existingPreference
        every { preferenceRepository.save(any()) } answers { firstArg() }

        // when
        val result = notificationService.updatePreference(request)

        // then
        assertThat(result.enabled).isFalse()

        verify(exactly = 1) { preferenceRepository.findByUserIdAndType(request.userId, request.type) }
        verify(exactly = 1) { preferenceRepository.save(any()) }
    }

    @Test
    fun `기존 알림 설정을 활성화할 수 있다`() {
        // given
        val request =
            UpdatePreferenceRequest(
                userId = 1L,
                type = NotificationType.ATTENDANCE_NUDGE,
                enabled = true,
            )

        val existingPreference =
            NotificationPreference.create(
                userId = request.userId,
                type = request.type,
                enabled = false,
            )

        every { preferenceRepository.findByUserIdAndType(request.userId, request.type) } returns existingPreference
        every { preferenceRepository.save(any()) } answers { firstArg() }

        // when
        val result = notificationService.updatePreference(request)

        // then
        assertThat(result.enabled).isTrue()

        verify(exactly = 1) { preferenceRepository.findByUserIdAndType(request.userId, request.type) }
        verify(exactly = 1) { preferenceRepository.save(any()) }
    }

    // ========== getUserPreferences Tests ==========

    @Test
    fun `사용자의 알림 설정 목록을 조회할 수 있다`() {
        // given
        val userId = 1L

        val preferences =
            listOf(
                NotificationPreference.create(
                    userId = userId,
                    type = NotificationType.GAME_START,
                    enabled = true,
                ),
                NotificationPreference.create(
                    userId = userId,
                    type = NotificationType.TEAM_NOTICE,
                    enabled = false,
                ),
                NotificationPreference.create(
                    userId = userId,
                    type = NotificationType.RECORD_ALERT,
                    enabled = true,
                ),
            )

        every { preferenceRepository.findByUserId(userId) } returns preferences

        // when
        val result = notificationService.getUserPreferences(userId)

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0].type).isEqualTo(NotificationType.GAME_START)
        assertThat(result[0].enabled).isTrue()
        assertThat(result[1].type).isEqualTo(NotificationType.TEAM_NOTICE)
        assertThat(result[1].enabled).isFalse()
        assertThat(result[2].type).isEqualTo(NotificationType.RECORD_ALERT)
        assertThat(result[2].enabled).isTrue()

        verify(exactly = 1) { preferenceRepository.findByUserId(userId) }
    }

    @Test
    fun `알림 설정이 없는 사용자는 빈 리스트를 반환한다`() {
        // given
        val userId = 1L

        every { preferenceRepository.findByUserId(userId) } returns emptyList()

        // when
        val result = notificationService.getUserPreferences(userId)

        // then
        assertThat(result).isEmpty()
    }
}
