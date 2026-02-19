package com.nextup.core.domain.attendance

/**
 * 불참 사유 열거형
 *
 * 출석 투표 시 불참/미정 응답에 대한 사유를 분류합니다.
 */
enum class AbsenceReason(
    val displayName: String,
) {
    /** 가족 행사 */
    FAMILY("가족 행사"),

    /** 부상 */
    INJURY("부상"),

    /** 업무 */
    WORK("업무"),

    /** 여행 */
    TRAVEL("여행"),

    /** 날씨 */
    WEATHER("날씨"),

    /** 기타 (reasonDetail 입력 가능) */
    OTHER("기타"),
}
