package com.nextup.core.domain.game

/**
 * 투수 승패 결정
 *
 * 투수에게 부여되는 승/패/세이브/홀드 결정을 정의합니다.
 */
enum class PitchingDecision(
    val displayName: String,
    val abbreviation: String,
    val description: String
) {
    WIN("승", "W", "승리투수"),
    LOSS("패", "L", "패전투수"),
    SAVE("세이브", "S", "세이브"),
    HOLD("홀드", "H", "홀드"),
    BLOWN_SAVE("블론세이브", "BS", "세이브 실패"),
    NONE("없음", "-", "결정 없음");

    /**
     * 승리 결정인지 확인합니다.
     */
    val isWin: Boolean
        get() = this == WIN

    /**
     * 패배 결정인지 확인합니다.
     */
    val isLoss: Boolean
        get() = this == LOSS

    /**
     * 세이브 결정인지 확인합니다.
     */
    val isSave: Boolean
        get() = this == SAVE

    /**
     * 홀드 결정인지 확인합니다.
     */
    val isHold: Boolean
        get() = this == HOLD

    /**
     * 블론세이브인지 확인합니다.
     */
    val isBlownSave: Boolean
        get() = this == BLOWN_SAVE

    /**
     * 결정이 없는지 확인합니다.
     */
    val hasNoDecision: Boolean
        get() = this == NONE

    /**
     * 구원 기록인지 확인합니다 (세이브 또는 홀드).
     */
    val isReliefSuccess: Boolean
        get() = this in listOf(SAVE, HOLD)
}
