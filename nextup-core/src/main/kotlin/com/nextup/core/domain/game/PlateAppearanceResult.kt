package com.nextup.core.domain.game

/**
 * 타석 결과
 *
 * 타자의 타석에서 발생할 수 있는 모든 결과를 정의합니다.
 * 결과는 타수에 포함되는지 여부(isAtBat)와 안타 여부(isHit)로 구분됩니다.
 */
enum class PlateAppearanceResult(
    val displayName: String,
    val description: String,
    val isAtBat: Boolean,
    val isHit: Boolean,
) {
    // 안타 (Hit) - 타수 O, 안타 O
    SINGLE("1루타", "안타로 1루에 진루", true, true),
    DOUBLE("2루타", "안타로 2루에 진루", true, true),
    TRIPLE("3루타", "안타로 3루에 진루", true, true),
    HOME_RUN("홈런", "안타로 홈까지 진루", true, true),

    // 아웃 (Out) - 타수 O, 안타 X
    STRIKEOUT("삼진", "3스트라이크 아웃", true, false),
    STRIKEOUT_DROPPED_THIRD("낫아웃 삼진", "낫아웃(포수가 제3스트라이크 포구 실패)으로 타자 출루", true, false),
    GROUND_OUT("땅볼 아웃", "땅볼로 아웃", true, false),
    FLY_OUT("플라이 아웃", "뜬공으로 아웃", true, false),
    INFIELD_FLY("인필드플라이", "내야 뜬공으로 아웃 (인필드플라이 룰 적용)", true, false),
    LINE_OUT("라인 드라이브 아웃", "직선타로 아웃", true, false),
    FIELDERS_CHOICE("야수 선택", "야수 선택으로 출루", true, false),
    ERROR("실책", "수비 실책으로 출루", true, false),
    DOUBLE_PLAY("병살타", "병살로 아웃", true, false),
    TRIPLE_PLAY("삼중살", "삼중살로 아웃", true, false),

    // 비타수 (No At-Bat) - 타수 X, 안타 X
    WALK("볼넷", "4볼로 출루", false, false),
    INTENTIONAL_WALK("고의4구", "고의 볼넷으로 출루", false, false),
    HIT_BY_PITCH("사구", "몸에 맞는 공으로 출루", false, false),
    SACRIFICE_BUNT("희생번트", "희생번트로 아웃", false, false),
    SACRIFICE_FLY("희생플라이", "희생플라이로 아웃", false, false),
    INTERFERENCE("방해", "수비 방해로 출루", false, false),
    ;

    /**
     * 단타인지 확인합니다.
     */
    val isSingle: Boolean
        get() = this == SINGLE

    /**
     * 2루타인지 확인합니다.
     */
    val isDouble: Boolean
        get() = this == DOUBLE

    /**
     * 3루타인지 확인합니다.
     */
    val isTriple: Boolean
        get() = this == TRIPLE

    /**
     * 홈런인지 확인합니다.
     */
    val isHomeRun: Boolean
        get() = this == HOME_RUN

    /**
     * 장타인지 확인합니다 (2루타 이상).
     */
    val isExtraBaseHit: Boolean
        get() = this in listOf(DOUBLE, TRIPLE, HOME_RUN)

    /**
     * 삼진인지 확인합니다 (낫아웃 삼진 포함).
     */
    val isStrikeout: Boolean
        get() = this == STRIKEOUT || this == STRIKEOUT_DROPPED_THIRD

    /**
     * 낫아웃 삼진인지 확인합니다.
     * 낫아웃 삼진은 삼진이지만 타자가 출루합니다.
     */
    val isDroppedThirdStrike: Boolean
        get() = this == STRIKEOUT_DROPPED_THIRD

    /**
     * 볼넷 또는 고의4구인지 확인합니다.
     */
    val isWalk: Boolean
        get() = this in listOf(WALK, INTENTIONAL_WALK)

    /**
     * 사구인지 확인합니다.
     */
    val isHitByPitch: Boolean
        get() = this == HIT_BY_PITCH

    /**
     * 희생타인지 확인합니다 (희생번트 또는 희생플라이).
     */
    val isSacrifice: Boolean
        get() = this in listOf(SACRIFICE_BUNT, SACRIFICE_FLY)

    /**
     * 출루에 성공했는지 확인합니다.
     */
    val isOnBase: Boolean
        get() =
            isHit ||
                this in
                listOf(
                    WALK,
                    INTENTIONAL_WALK,
                    HIT_BY_PITCH,
                    FIELDERS_CHOICE,
                    ERROR,
                    INTERFERENCE,
                    STRIKEOUT_DROPPED_THIRD,
                )

    /**
     * 루타 수를 반환합니다 (안타가 아니면 0).
     */
    val totalBases: Int
        get() =
            when (this) {
                SINGLE -> 1
                DOUBLE -> 2
                TRIPLE -> 3
                HOME_RUN -> 4
                else -> 0
            }
}
