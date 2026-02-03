package com.nextup.core.domain.game

/**
 * 주루 이벤트
 *
 * 타석 결과와 별개로 발생하는 주루 관련 이벤트를 정의합니다.
 */
enum class BaseRunningEvent(
    val displayName: String,
    val description: String
) {
    // 도루 관련
    STOLEN_BASE("도루", "도루 성공"),
    CAUGHT_STEALING("도루 실패", "도루 시도 중 아웃"),
    DOUBLE_STEAL("더블 스틸", "2명 동시 도루"),
    TRIPLE_STEAL("트리플 스틸", "3명 동시 도루"),

    // 투수 관련 진루
    WILD_PITCH("폭투", "투수 폭투로 주자 진루"),
    PASSED_BALL("포일", "포수 포일로 주자 진루"),
    BALK("보크", "보크로 주자 진루"),

    // 견제/픽오프
    PICKOFF("견제사", "견제로 아웃"),
    PICKOFF_ERROR("견제 실책", "견제 실책으로 주자 진루"),

    // 기타 진루
    ADVANCE_ON_THROW("송구 사이 진루", "송구 사이 추가 진루"),
    ADVANCE_ON_ERROR("실책 진루", "수비 실책으로 추가 진루"),
    ADVANCE_ON_FLYOUT("태그업", "플라이 아웃 후 태그업 진루"),
    ADVANCE_ON_GROUND_OUT("땅볼 사이 진루", "땅볼 아웃 사이 진루"),

    // 주루 아웃
    OUT_ON_BASES("주루사", "주루 중 아웃"),
    OUT_ON_APPEAL("어필 아웃", "어필 플레이로 아웃"),
    OUT_PASSING_RUNNER("추월 아웃", "앞 주자 추월로 아웃"),
    OUT_INTERFERENCE("주루 방해", "주루 중 수비 방해로 아웃");

    /**
     * 도루 관련 이벤트인지 확인합니다.
     */
    val isStealAttempt: Boolean
        get() = this in listOf(STOLEN_BASE, CAUGHT_STEALING, DOUBLE_STEAL, TRIPLE_STEAL)

    /**
     * 도루 성공인지 확인합니다.
     */
    val isSuccessfulSteal: Boolean
        get() = this in listOf(STOLEN_BASE, DOUBLE_STEAL, TRIPLE_STEAL)

    /**
     * 투수/포수 책임 진루인지 확인합니다.
     */
    val isBatteryError: Boolean
        get() = this in listOf(WILD_PITCH, PASSED_BALL, BALK)

    /**
     * 아웃 이벤트인지 확인합니다.
     */
    val isOut: Boolean
        get() = this in listOf(
            CAUGHT_STEALING, PICKOFF,
            OUT_ON_BASES, OUT_ON_APPEAL, OUT_PASSING_RUNNER, OUT_INTERFERENCE
        )

    /**
     * 진루 이벤트인지 확인합니다.
     */
    val isAdvance: Boolean
        get() = this in listOf(
            STOLEN_BASE, DOUBLE_STEAL, TRIPLE_STEAL,
            WILD_PITCH, PASSED_BALL, BALK,
            PICKOFF_ERROR, ADVANCE_ON_THROW, ADVANCE_ON_ERROR,
            ADVANCE_ON_FLYOUT, ADVANCE_ON_GROUND_OUT
        )
}
