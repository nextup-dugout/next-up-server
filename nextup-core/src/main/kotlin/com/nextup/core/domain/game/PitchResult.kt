package com.nextup.core.domain.game

/**
 * 투구 결과
 *
 * 개별 투구의 결과를 정의합니다.
 */
enum class PitchResult(
    val displayName: String,
    val description: String,
    val affectsBall: Boolean,
    val affectsStrike: Boolean,
) {
    BALL("볼", "스트라이크 존 밖으로 벗어난 투구", true, false),
    STRIKE("스트라이크", "정규 스트라이크 (헛스윙 제외)", false, true),
    FOUL("파울", "파울 지역으로 타구", false, false),
    SWING_MISS("헛스윙", "타자가 스윙했으나 공을 맞추지 못함", false, true),
    IN_PLAY("인플레이", "타구가 페어 지역에 들어감", false, false),
    ;

    /**
     * 볼카운트에 영향을 주는지 확인합니다.
     */
    val incrementsBall: Boolean
        get() = affectsBall

    /**
     * 스트라이크 카운트에 영향을 주는지 확인합니다.
     */
    val incrementsStrike: Boolean
        get() = affectsStrike

    /**
     * 파울인지 확인합니다.
     */
    val isFoul: Boolean
        get() = this == FOUL

    /**
     * 인플레이인지 확인합니다.
     */
    val isInPlay: Boolean
        get() = this == IN_PLAY
}
