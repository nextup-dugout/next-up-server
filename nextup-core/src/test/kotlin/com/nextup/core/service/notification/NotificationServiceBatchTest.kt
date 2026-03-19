package com.nextup.core.service.notification

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

@DisplayName("NotificationService 배치 알림 테스트")
class NotificationServiceBatchTest {
    private val notificationRepository: NotificationRepositoryPort = mockk()
    private val deviceTokenRepository: DeviceTokenRepositoryPort = mockk()
    private val preferenceRepository: NotificationPreferenceRepositoryPort = mockk()
    private val pushNotificationPort: PushNotificationPort = mockk()

    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationService =
            NotificationService(
                notificationRepository = notificationRepository,
                deviceTokenRepository = deviceTokenRepository,
                preferenceRepository = preferenceRepository,
                pushNotificationPort = pushNotificationPort,
            )
    }

    @Test
    fun `sendBatchNotifications는 모든 알림을 저장한다`() {
        // given
        val requests =
            listOf(
                SendNotificationRequest(
                    userId = 1L,
                    type = NotificationType.ATTENDANCE_VOTE_CREATED,
                    title = "출석 투표",
                    body = "투표가 생성되었습니다.",
                ),
                SendNotificationRequest(
                    userId = 2L,
                    type = NotificationType.ATTENDANCE_VOTE_CREATED,
                    title = "출석 투표",
                    body = "투표가 생성되었습니다.",
                ),
                SendNotificationRequest(
                    userId = 3L,
                    type = NotificationType.ATTENDANCE_VOTE_CREATED,
                    title = "출석 투표",
                    body = "투표가 생성되었습니다.",
                ),
            )

        every { notificationRepository.save(any()) } answers { firstArg() }
        every { deviceTokenRepository.findByUserId(any()) } returns emptyList()

        // when
        val result = notificationService.sendBatchNotifications(requests)

        // then
        assertThat(result).hasSize(3)
        verify(exactly = 3) { notificationRepository.save(any()) }
    }

    @Test
    fun `sendBatchNotifications에 빈 리스트를 전달하면 빈 리스트를 반환한다`() {
        // when
        val result = notificationService.sendBatchNotifications(emptyList())

        // then
        assertThat(result).isEmpty()
        verify(exactly = 0) { notificationRepository.save(any()) }
    }

    @Test
    fun `sendBatchNotifications는 각 사용자에게 푸시를 발송한다`() {
        // given
        val requests =
            listOf(
                SendNotificationRequest(
                    userId = 1L,
                    type = NotificationType.GAME_RESULT_CONFIRMED,
                    title = "경기 결과",
                    body = "5 - 3",
                ),
                SendNotificationRequest(
                    userId = 2L,
                    type = NotificationType.GAME_RESULT_CONFIRMED,
                    title = "경기 결과",
                    body = "5 - 3",
                ),
            )

        every { notificationRepository.save(any()) } answers { firstArg() }
        every { deviceTokenRepository.findByUserId(any()) } returns emptyList()

        // when
        notificationService.sendBatchNotifications(requests)

        // then
        verify(exactly = 1) { deviceTokenRepository.findByUserId(1L) }
        verify(exactly = 1) { deviceTokenRepository.findByUserId(2L) }
    }
}
