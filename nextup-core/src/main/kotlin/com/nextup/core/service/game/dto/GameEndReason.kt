package com.nextup.core.service.game.dto

/**
 * 경기 종료 사유
 */
enum class GameEndReason(
    val displayName: String,
    val description: String,
) {
    /** 정규 이닝 종료 */
    REGULATION("정규 종료", "정규 이닝 완료로 경기 종료"),

    /** 콜드게임 (점수 차이) */
    MERCY_RULE("콜드게임 (점수차)", "점수 차이로 인한 콜드게임"),

    /** 콜드게임 (기상 조건) */
    WEATHER("콜드게임 (날씨)", "기상 조건으로 인한 콜드게임"),

    /** 몰수 */
    FORFEIT("몰수", "몰수 처리"),

    /** 기타 */
    OTHER("기타", "기타 사유로 경기 종료"),
}
