package com.nextup.core.domain.attendance

/**
 * 이벤트 카테고리
 *
 * 출석 투표의 이벤트 유형을 분류합니다.
 */
enum class EventCategory(
    val displayName: String,
) {
    /** 경기 */
    GAME("경기"),

    /** 연습 */
    PRACTICE("연습"),

    /** 회식 */
    DINNER("회식"),

    /** 모임 */
    MEETING("모임"),

    /** 번개 */
    PICKUP("번개"),

    /** 기타 */
    OTHER("기타"),
}
