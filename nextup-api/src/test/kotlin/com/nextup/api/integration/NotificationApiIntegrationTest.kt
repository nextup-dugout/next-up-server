package com.nextup.api.integration

import com.nextup.core.domain.notification.DevicePlatform
import com.nextup.core.domain.notification.DeviceToken
import com.nextup.core.domain.notification.Notification
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.port.repository.DeviceTokenRepositoryPort
import com.nextup.core.port.repository.NotificationRepositoryPort
import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@AutoConfigureMockMvc
@Transactional
class NotificationApiIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var notificationRepository: NotificationRepositoryPort

    @Autowired
    private lateinit var deviceTokenRepository: DeviceTokenRepositoryPort

    private fun createToken(userId: Long): String =
        jwtTokenProvider.createAccessToken(
            userId = userId,
            email = "test@test.com",
            roles = setOf("ROLE_USER"),
        )

    @Test
    fun `알림 생성 후 미읽음 수 조회가 정상 동작한다`() {
        // given
        val userId = 100L
        val notification =
            Notification.create(
                userId = userId,
                type = NotificationType.TEAM_NOTICE,
                title = "테스트 알림",
                body = "테스트 알림 내용입니다",
            )
        notification.markAsSent()
        notificationRepository.save(notification)

        val token = createToken(userId)

        // when & then
        val result =
            mockMvc
                .perform(
                    get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer $token"),
                )
                .andExpect(status().isOk)
                .andReturn()

        assertThat(result.response.contentAsString).contains("\"count\":1")
    }

    @Test
    fun `디바이스 토큰 등록 후 목록 조회가 정상 동작한다`() {
        // given
        val userId = 200L
        val device =
            DeviceToken.create(
                userId = userId,
                token = "fcm-integration-test-token",
                platform = DevicePlatform.ANDROID,
            )
        deviceTokenRepository.save(device)

        // when
        val devices = deviceTokenRepository.findByUserId(userId)

        // then
        assertThat(devices).hasSize(1)
        assertThat(devices[0].token).isEqualTo("fcm-integration-test-token")
        assertThat(devices[0].platform).isEqualTo(DevicePlatform.ANDROID)
    }

    @Test
    fun `알림 목록 API 조회가 정상 동작한다`() {
        // given
        val userId = 300L
        repeat(3) { i ->
            val notification =
                Notification.create(
                    userId = userId,
                    type = NotificationType.GAME_START,
                    title = "알림 ${i + 1}",
                    body = "알림 내용 ${i + 1}",
                )
            notification.markAsSent()
            notificationRepository.save(notification)
        }

        val token = createToken(userId)

        // when & then
        mockMvc
            .perform(
                get("/api/v1/notifications")
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer $token"),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
    }
}
