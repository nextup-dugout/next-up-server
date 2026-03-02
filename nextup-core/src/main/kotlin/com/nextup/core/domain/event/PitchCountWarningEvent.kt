package com.nextup.core.domain.event

/**
 * 투구 수 제한 경고 이벤트
 *
 * 투수의 투구 수가 제한에 임박하거나 초과했을 때 발행됩니다.
 * 사회인 야구 투수 보호 규칙 지원을 위한 알림 이벤트입니다.
 *
 * @property gameId 경기 ID
 * @property gamePlayerId 투수 GamePlayer ID
 * @property playerId 투수 Player ID
 * @property pitchesThrown 현재까지 던진 투구 수
 * @property pitchCountLimit 투구 수 제한
 * @property warningType 경고 유형
 */
data class PitchCountWarningEvent(
    val gameId: Long,
    val gamePlayerId: Long,
    val playerId: Long,
    val pitchesThrown: Int,
    val pitchCountLimit: Int,
    val warningType: PitchCountWarningType,
) {
    /** 남은 투구 수 (제한 초과 시 음수) */
    val remainingPitches: Int
        get() = pitchCountLimit - pitchesThrown
}

/**
 * 투구 수 경고 유형
 */
enum class PitchCountWarningType(
    val displayName: String,
) {
    /** 투구 수 제한 임박 (threshold 이하 남음) */
    APPROACHING_LIMIT("투구 수 제한 임박"),

    /** 투구 수 제한 도달 */
    LIMIT_REACHED("투구 수 제한 도달"),
}
