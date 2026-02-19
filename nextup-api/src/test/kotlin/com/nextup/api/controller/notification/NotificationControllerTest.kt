package com.nextup.api.controller.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.notification.RegisterDeviceApiRequest
import com.nextup.api.dto.notification.UpdatePreferenceApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.DeviceTokenNotFoundException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.NotificationNotFoundException
import com.nextup.core.domain.notification.*
import com.nextup.core.service.notification.NotificationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

@DisplayName("NotificationController")
class NotificationControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var notificationService: NotificationService
    private lateinit var objectMapper: ObjectMapper

    private val mockNotification = mockk<Notification>(relaxed = true)
    private val mockDeviceToken = mockk<DeviceToken>(relaxed = true)
    private val mockPreference = mockk<NotificationPreference>(relaxed = true)
    private val authenticatedUserId = 100L

    @BeforeEach
    fun setUp() {
        notificationService = mockk()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        val controller = NotificationController(notificationService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                    PageableHandlerMethodArgumentResolver(),
                    AuthenticationPrincipalResolver(authenticatedUserId),
                )
                .build()

        every { mockNotification.id } returns 1L
        every { mockNotification.userId } returns authenticatedUserId
        every { mockNotification.type } returns NotificationType.GAME_START
        every { mockNotification.title } returns "경기 시작"
        every { mockNotification.body } returns "30분 후 경기가 시작됩니다"
        every { mockNotification.data } returns """{"gameId": 123}"""
        every { mockNotification.readAt } returns null
        every { mockNotification.sentAt } returns null
        every { mockNotification.createdAt } returns Instant.now()

        every { mockDeviceToken.id } returns 1L
        every { mockDeviceToken.userId } returns authenticatedUserId
        every { mockDeviceToken.token } returns "fcm-token-12345"
        every { mockDeviceToken.platform } returns DevicePlatform.IOS
        every { mockDeviceToken.createdAt } returns Instant.now()

        every { mockPreference.id } returns 1L
        every { mockPreference.userId } returns authenticatedUserId
        every { mockPreference.type } returns NotificationType.GAME_START
        every { mockPreference.enabled } returns true
        every { mockPreference.createdAt } returns Instant.now()
    }

    @Test
    fun `should register device successfully`() {
        // given
        val request =
            RegisterDeviceApiRequest(
                token = "fcm-token-12345",
                platform = DevicePlatform.IOS,
            )

        every { notificationService.registerDevice(any()) } returns mockDeviceToken

        // when & then
        mockMvc
            .perform(
                post("/api/v1/devices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.userId").value(authenticatedUserId))
            .andExpect(jsonPath("$.data.token").value("fcm-token-12345"))
            .andExpect(jsonPath("$.data.platform").value("IOS"))

        verify(exactly = 1) { notificationService.registerDevice(any()) }
    }

    @Test
    fun `should fail to register device with invalid request`() {
        // given
        val invalidRequest =
            mapOf(
                "token" to "",
                "platform" to "INVALID",
            )

        // when & then
        mockMvc
            .perform(
                post("/api/v1/devices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))

        verify(exactly = 0) { notificationService.registerDevice(any()) }
    }

    @Test
    fun `should remove device successfully`() {
        // given
        every { notificationService.removeDevice(1L, authenticatedUserId) } returns Unit

        // when & then
        mockMvc
            .perform(delete("/api/v1/devices/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(exactly = 1) { notificationService.removeDevice(1L, authenticatedUserId) }
    }

    @Test
    fun `should fail to remove device when not found`() {
        // given
        every {
            notificationService.removeDevice(999L, authenticatedUserId)
        } throws DeviceTokenNotFoundException(999L)

        // when & then
        mockMvc
            .perform(delete("/api/v1/devices/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should fail to remove device when not owned`() {
        // given
        every {
            notificationService.removeDevice(1L, authenticatedUserId)
        } throws ForbiddenException("DEVICE_TOKEN_ACCESS_DENIED", "해당 디바이스 토큰에 접근 권한이 없습니다")

        // when & then
        mockMvc
            .perform(delete("/api/v1/devices/1"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should get notifications successfully`() {
        // given
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(mockNotification), pageable, 1)

        every { notificationService.getUserNotifications(authenticatedUserId, any()) } returns page

        // when & then
        mockMvc
            .perform(
                get("/api/v1/notifications")
                    .param("page", "0")
                    .param("size", "20"),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].userId").value(authenticatedUserId))
            .andExpect(jsonPath("$.data[0].type").value("GAME_START"))
            .andExpect(jsonPath("$.data[0].title").value("경기 시작"))

        verify(exactly = 1) { notificationService.getUserNotifications(authenticatedUserId, any()) }
    }

    @Test
    fun `should mark notification as read successfully`() {
        // given
        every { notificationService.markAsRead(1L, authenticatedUserId) } returns mockNotification

        // when & then
        mockMvc
            .perform(put("/api/v1/notifications/1/read"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))

        verify(exactly = 1) { notificationService.markAsRead(1L, authenticatedUserId) }
    }

    @Test
    fun `should fail to mark notification as read when not found`() {
        // given
        every {
            notificationService.markAsRead(999L, authenticatedUserId)
        } throws NotificationNotFoundException(999L)

        // when & then
        mockMvc
            .perform(put("/api/v1/notifications/999/read"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should fail to mark notification as read when not owned`() {
        // given
        every {
            notificationService.markAsRead(1L, authenticatedUserId)
        } throws ForbiddenException("NOTIFICATION_ACCESS_DENIED", "해당 알림에 접근 권한이 없습니다")

        // when & then
        mockMvc
            .perform(put("/api/v1/notifications/1/read"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should update preference successfully`() {
        // given
        val request =
            UpdatePreferenceApiRequest(
                type = NotificationType.GAME_START,
                enabled = false,
            )

        every { notificationService.updatePreference(any()) } returns mockPreference

        // when & then
        mockMvc
            .perform(
                put("/api/v1/notifications/preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.userId").value(authenticatedUserId))
            .andExpect(jsonPath("$.data.type").value("GAME_START"))

        verify(exactly = 1) { notificationService.updatePreference(any()) }
    }

    @Test
    fun `should get preferences successfully`() {
        // given
        every { notificationService.getUserPreferences(authenticatedUserId) } returns listOf(mockPreference)

        // when & then
        mockMvc
            .perform(get("/api/v1/notifications/preferences"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].userId").value(authenticatedUserId))
            .andExpect(jsonPath("$.data[0].type").value("GAME_START"))
            .andExpect(jsonPath("$.data[0].enabled").value(true))

        verify(exactly = 1) { notificationService.getUserPreferences(authenticatedUserId) }
    }
}

/**
 * 테스트용 @AuthenticationPrincipal 리졸버
 */
private class AuthenticationPrincipalResolver(
    private val userId: Long,
) : org.springframework.web.method.support.HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: org.springframework.core.MethodParameter): Boolean =
        parameter.hasParameterAnnotation(
            org.springframework.security.core.annotation.AuthenticationPrincipal::class.java,
        )

    override fun resolveArgument(
        parameter: org.springframework.core.MethodParameter,
        mavContainer: org.springframework.web.method.support.ModelAndViewContainer?,
        webRequest: org.springframework.web.context.request.NativeWebRequest,
        binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
    ): Any = userId
}
