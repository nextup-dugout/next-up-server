package com.nextup.core.domain.game

/**
 * 특수 경기 기록 유형
 *
 * 경기 종료 후 자동으로 감지되는 특수 기록을 나타냅니다.
 */
enum class SpecialGameRecord(
    val displayName: String,
    val description: String,
) {
    /** 노히트 노런 (안타 허용 0, 볼넷/사구/실책은 허용 가능) */
    NO_HITTER("노히트노런", "상대 팀에게 안타를 하나도 허용하지 않은 경기"),

    /** 퍼펙트 게임 (안타 0, 볼넷 0, 사구 0, 실책 0 - 모든 주자 출루 없음) */
    PERFECT_GAME("퍼펙트게임", "상대 팀의 모든 타자를 한 명도 출루시키지 않은 경기"),
}
