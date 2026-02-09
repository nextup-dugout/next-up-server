package com.nextup.api.dto.notification

import com.nextup.core.domain.notification.NotificationType
import jakarta.validation.constraints.NotNull

/**
 * 알림 설정 수정 요청 DTO
 */
data class UpdatePreferenceApiRequest(
    @field:NotNull(message = "알림 타입은 필수입니다")
    val type: NotificationType,
    @field:NotNull(message = "활성화 여부는 필수입니다")
    val enabled: Boolean,
)
