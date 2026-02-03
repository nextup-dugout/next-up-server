package com.nextup.core.domain.game

/**
 * 선수 교체 유형
 *
 * 경기 중 발생하는 선수 교체의 유형을 정의합니다.
 */
enum class SubstitutionType(
    val displayName: String,
    val description: String
) {
    // 타자 교체
    PINCH_HITTER("대타", "타순 대신 타격"),
    PINCH_RUNNER("대주자", "주자 대신 주루"),

    // 투수 교체
    PITCHING_CHANGE("투수 교체", "투수 교체"),

    // 수비 교체
    DEFENSIVE_REPLACEMENT("수비 교체", "수비 위치 교체"),
    POSITION_SWITCH("포지션 변경", "출전 중 선수들의 포지션 교환"),

    // DH 관련
    DH_INTO_FIELD("DH 수비 투입", "DH가 수비 위치로 이동 (DH 규칙 해제)"),
    PITCHER_TO_BATTING_ORDER("투수 타순 진입", "투수가 타순에 진입 (DH 규칙 해제)"),

    // 특수 상황
    DOUBLE_SWITCH("더블 스위치", "투수 교체와 타순 변경 동시 진행"),
    LINEUP_CORRECTION("라인업 정정", "라인업 오류 정정");

    /**
     * 타자 관련 교체인지 확인합니다.
     */
    val isBatterSubstitution: Boolean
        get() = this in listOf(PINCH_HITTER, PINCH_RUNNER)

    /**
     * 투수 교체인지 확인합니다.
     */
    val isPitchingChange: Boolean
        get() = this == PITCHING_CHANGE || this == DOUBLE_SWITCH

    /**
     * 수비 교체인지 확인합니다.
     */
    val isDefensiveChange: Boolean
        get() = this in listOf(DEFENSIVE_REPLACEMENT, POSITION_SWITCH, DH_INTO_FIELD)

    /**
     * DH 규칙 관련 교체인지 확인합니다.
     */
    val isDHRelated: Boolean
        get() = this in listOf(DH_INTO_FIELD, PITCHER_TO_BATTING_ORDER)
}
