package com.nextup.scorer.dto.fielding

import jakarta.validation.constraints.NotNull

/**
 * 수비 이벤트 요청 DTO
 *
 * 실시간 경기 기록 시 수비 이벤트를 입력하기 위한 요청입니다.
 */
data class FieldingEventRequest(
    @field:NotNull(message = "gamePlayerId는 필수입니다.")
    val gamePlayerId: Long?,
    @field:NotNull(message = "eventType은 필수입니다.")
    val eventType: FieldingEventType?,
)

/**
 * 수비 이벤트 유형
 */
enum class FieldingEventType(
    val displayName: String,
) {
    PUT_OUT("자살"),
    ASSIST("보살"),
    ERROR("실책"),
    DOUBLE_PLAY("병살 관여"),
    PASSED_BALL("포일"),
}
