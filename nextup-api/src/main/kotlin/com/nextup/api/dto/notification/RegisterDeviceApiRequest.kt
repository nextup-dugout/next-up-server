package com.nextup.api.dto.notification

import com.nextup.core.domain.notification.DevicePlatform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 디바이스 토큰 등록 요청 DTO
 */
data class RegisterDeviceApiRequest(
    @field:NotBlank(message = "토큰은 필수입니다")
    val token: String,
    @field:NotNull(message = "플랫폼은 필수입니다")
    val platform: DevicePlatform,
)
