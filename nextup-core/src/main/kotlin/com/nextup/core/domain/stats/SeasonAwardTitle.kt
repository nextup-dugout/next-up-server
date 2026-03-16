package com.nextup.core.domain.stats

/**
 * 시즌 타이틀(개인상) 종류
 *
 * 시즌 종료 시 각 부문별 최고 성적을 거둔 선수에게 부여됩니다.
 */
enum class SeasonAwardTitle(
    val displayName: String,
    val description: String,
) {
    BATTING_CHAMPION("타격왕", "규정타석 이상 최고 타율"),
    HOME_RUN_KING("홈런왕", "최다 홈런"),
    RBI_KING("타점왕", "최다 타점"),
    STOLEN_BASE_KING("도루왕", "최다 도루"),
    WINS_LEADER("다승왕", "최다 승리"),
    ERA_TITLE("방어율 1위", "규정이닝 이상 최저 방어율"),
    STRIKEOUT_KING("탈삼진왕", "최다 탈삼진"),
    SAVES_LEADER("세이브왕", "최다 세이브"),
    HITS_LEADER("최다안타", "최다 안타"),
    MVP("MVP", "최우수선수"),
}
