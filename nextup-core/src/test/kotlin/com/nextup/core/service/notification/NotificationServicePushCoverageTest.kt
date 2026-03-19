package com.nextup.core.service.notification

import com.nextup.core.domain.notification.DevicePlatform
import com.nextup.core.domain.notification.DeviceToken
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.port.repository.DeviceTokenRepositoryPort
import com.nextup.core.port.repository.NotificationPreferenceRepositoryPort
import com.nextup.core.port.repository.NotificationRepositoryPort
import com.nextup.core.port.service.PushNotificationPort
import com.nextup.core.service.notification.dto.SendNotificationRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NotificationService sendPushToUser 커버리지 보완")
class NotificationServicePushCoverageTest {
    private lateinit var notificationRepository: NotificationRepositoryPort
    private lateinit var deviceTokenRepository: DeviceTokenRepositoryPort
    private lateinit var preferenceRepository: NotificationPreferenceRepositoryPort
    private lateinit var pushNotificationPort: PushNotificationPort
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationRepository = mockk()
        deviceTokenRepository = mockk()
        preferenceRepository = mockk()
        pushNotificationPort = mockk(relaxed = true)
        notificationService =
            NotificationService(
                notificationRepository,
                deviceTokenRepository,
                preferenceRepository,
                pushNotificationPort,
            )
    }

    @Test
    fun `토큰이 있으면 푸시 발송을 호출한다 - data 있는 경우`() {
        // given
        val request =
            SendNotificationRequest(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "경기가 곧 시작됩니다",
                data = """{"gameId": 123}""",
            )

        val tokens =
            listOf(
                DeviceToken.create(
                    userId = 1L,
                    token = "token-1",
                    platform = DevicePlatform.ANDROID,
                ),
                DeviceToken.create(
                    userId = 1L,
                    token = "token-2",
                    platform = DevicePlatform.IOS,
                ),
            )

        every { preferenceRepository.findByUserIdAndType(1L, NotificationType.GAME_START) } returns null
        every { notificationRepository.save(any()) } answers { firstArg() }
        every { deviceTokenRepository.findByUserId(1L) } returns tokens
        every { pushNotificationPort.sendBatch(any(), any(), any(), any()) } returns 2

        // when
        val result = notificationService.sendNotification(request)

        // then
        assertThat(result).isNotNull
        assertThat(result!!.isSent()).isTrue()
        verify(exactly = 1) {
            pushNotificationPort.sendBatch(
                tokens = listOf("token-1", "token-2"),
                title = "경기 시작",
                body = "경기가 곧 시작됩니다",
                data = mapOf("data" to """{"gameId": 123}"""),
            )
        }
    }

    @Test
    fun `토큰이 있으면 푸시 발송을 호출한다 - data null인 경우`() {
        // given
        val request =
            SendNotificationRequest(
                userId = 1L,
                type = NotificationType.TEAM_NOTICE,
                title = "팀 공지",
                body = "내용",
                data = null,
            )

        val tokens =
            listOf(
                DeviceToken.create(
                    userId = 1L,
                    token = "token-1",
                    platform = DevicePlatform.ANDROID,
                ),
            )

        every { preferenceRepository.findByUserIdAndType(1L, NotificationType.TEAM_NOTICE) } returns null
        every { notificationRepository.save(any()) } answers { firstArg() }
        every { deviceTokenRepository.findByUserId(1L) } returns tokens
        every { pushNotificationPort.sendBatch(any(), any(), any(), any()) } returns 1

        // when
        val result = notificationService.sendNotification(request)

        // then
        assertThat(result).isNotNull
        assertThat(result!!.isSent()).isTrue()
        verify(exactly = 1) {
            pushNotificationPort.sendBatch(
                tokens = listOf("token-1"),
                title = "팀 공지",
                body = "내용",
                data = null,
            )
        }
    }

    @Test
    fun `푸시 발송 중 예외 발생 시 알림 저장은 유지된다`() {
        // given
        val request =
            SendNotificationRequest(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "내용",
                data = null,
            )

        val tokens =
            listOf(
                DeviceToken.create(
                    userId = 1L,
                    token = "token-1",
                    platform = DevicePlatform.ANDROID,
                ),
            )

        every { preferenceRepository.findByUserIdAndType(1L, NotificationType.GAME_START) } returns null
        every { notificationRepository.save(any()) } answers { firstArg() }
        every { deviceTokenRepository.findByUserId(1L) } returns tokens
        every {
            pushNotificationPort.sendBatch(any(), any(), any(), any())
        } throws RuntimeException("FCM error")

        // when
        val result = notificationService.sendNotification(request)

        // then - 알림은 정상적으로 저장되어야 한다
        assertThat(result).isNotNull
        assertThat(result!!.isSent()).isTrue()
        verify(exactly = 1) { notificationRepository.save(any()) }
    }
}
