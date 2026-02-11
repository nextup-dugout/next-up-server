package com.nextup.core.service.title

/**
 * 개인 타이틀 카테고리
 */
enum class TitleCategory(
    val displayName: String,
    val isQualificationRequired: Boolean,
) {
    BATTING_AVG("타격왕", true),
    HOME_RUNS("홈런왕", false),
    RBI("타점왕", false),
    STOLEN_BASES("도루왕", false),
    HITS("최다안타", false),
    WINS("다승왕", false),
    ERA("평균자책점", true),
    SAVES("세이브왕", false),
    STRIKEOUTS("탈삼진왕", false),
}
