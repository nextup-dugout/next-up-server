package com.nextup.core.service.notification.dto

import com.nextup.core.domain.notification.DevicePlatform

/**
 * 디바이스 등록 요청 DTO
 */
data class RegisterDeviceRequest(
    val userId: Long,
    val token: String,
    val platform: DevicePlatform,
)
