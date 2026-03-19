package com.nextup.api.controller.notification

import com.nextup.api.dto.common.PagedResponse
import com.nextup.api.dto.notification.DeviceTokenResponse
import com.nextup.api.dto.notification.NotificationPreferenceResponse
import com.nextup.api.dto.notification.NotificationResponse
import com.nextup.api.dto.notification.RegisterDeviceApiRequest
import com.nextup.api.dto.notification.UnreadCountResponse
import com.nextup.api.dto.notification.UpdatePreferenceApiRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.notification.NotificationService
import com.nextup.core.service.notification.dto.RegisterDeviceRequest
import com.nextup.core.service.notification.dto.UpdatePreferenceRequest
import com.nextup.infrastructure.common.toPageCommand
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 알림 API Controller
 *
 * 사용자의 알림 조회, 디바이스 관리, 알림 설정 관리
 * 모든 엔드포인트에서 JWT 인증된 사용자 ID를 사용합니다.
 */
@RestController
@RequestMapping("/api/v1")
class NotificationController(
    private val notificationService: NotificationService,
) {
    /**
     * 디바이스 토큰을 등록합니다.
     */
    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerDevice(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: RegisterDeviceApiRequest,
    ): ApiResponse<DeviceTokenResponse> {
        val deviceToken =
            notificationService.registerDevice(
                RegisterDeviceRequest(
                    userId = userId,
                    token = request.token,
                    platform = request.platform,
                ),
            )

        return ApiResponse.success(DeviceTokenResponse.from(deviceToken))
    }

    /**
     * 디바이스 토큰을 삭제합니다.
     */
    @DeleteMapping("/devices/{tokenId}")
    fun removeDevice(
        @AuthenticationPrincipal userId: Long,
        @PathVariable tokenId: Long,
    ): ApiResponse<Unit> {
        notificationService.removeDevice(tokenId, userId)
        return ApiResponse.success(Unit)
    }

    /**
     * 사용자의 알림 목록을 조회합니다.
     */
    @GetMapping("/notifications")
    fun getNotifications(
        @AuthenticationPrincipal userId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<PagedResponse<NotificationResponse>> {
        val notifications = notificationService.getUserNotifications(userId, pageable.toPageCommand())
        return ApiResponse.success(PagedResponse.from(notifications.map { NotificationResponse.from(it) }))
    }

    /**
     * 알림을 읽음 처리합니다.
     */
    @PutMapping("/notifications/{id}/read")
    fun markAsRead(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ): ApiResponse<NotificationResponse> {
        val notification = notificationService.markAsRead(id, userId)
        return ApiResponse.success(NotificationResponse.from(notification))
    }

    /**
     * 알림 설정을 수정합니다.
     */
    @PutMapping("/notifications/preferences")
    fun updatePreference(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: UpdatePreferenceApiRequest,
    ): ApiResponse<NotificationPreferenceResponse> {
        val preference =
            notificationService.updatePreference(
                UpdatePreferenceRequest(
                    userId = userId,
                    type = request.type,
                    enabled = request.enabled,
                ),
            )

        return ApiResponse.success(NotificationPreferenceResponse.from(preference))
    }

    /**
     * 사용자의 알림 설정 목록을 조회합니다.
     */
    @GetMapping("/notifications/preferences")
    fun getPreferences(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<NotificationPreferenceResponse>> {
        val preferences = notificationService.getUserPreferences(userId)
        return ApiResponse.success(preferences.map { NotificationPreferenceResponse.from(it) })
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     *
     * PATCH /api/v1/notifications/read-all
     */
    @PatchMapping("/notifications/read-all")
    fun markAllAsRead(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<Unit> {
        notificationService.markAllAsRead(userId)
        return ApiResponse.success(Unit)
    }

    /**
     * 미읽은 알림 개수를 조회합니다. (A-06)
     *
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/notifications/unread-count")
    fun getUnreadCount(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<UnreadCountResponse> {
        val count = notificationService.getUnreadCount(userId)
        return ApiResponse.success(UnreadCountResponse(count = count))
    }
}
