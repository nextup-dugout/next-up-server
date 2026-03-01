package com.nextup.core.domain.game

/**
 * 주루 플레이 결과 유형
 */
enum class BaseRunningResult(
    val displayName: String,
) {
    STOLEN_BASE("도루 성공"),
    CAUGHT_STEALING("도루 실패"),
    PICKED_OFF("견제사"),
    ADVANCED_ON_ERROR("실책으로 진루"),
    ADVANCED_ON_WILD_PITCH("폭투 진루"),
    ADVANCED_ON_PASSED_BALL("포일 진루"),
    ADVANCED_ON_BALK("보크 진루"),
}
