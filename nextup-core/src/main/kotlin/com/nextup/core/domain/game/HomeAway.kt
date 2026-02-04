package com.nextup.core.domain.game

/**
 * 홈/원정 구분
 */
enum class HomeAway(
    val displayName: String,
    val isHome: Boolean,
) {
    /** 홈팀 (후공) */
    HOME("홈", true),

    /** 원정팀 (선공) */
    AWAY("원정", false),
    ;

    /**
     * 반대편을 반환합니다.
     */
    fun opposite(): HomeAway = if (this == HOME) AWAY else HOME
}
