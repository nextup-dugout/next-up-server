package com.nextup.core.domain.eventgame

/**
 * 이벤트 게임 참가자 상태
 */
enum class EventGameParticipantStatus(
    val displayName: String,
) {
    /** 참가 신청됨 */
    APPLIED("신청됨"),

    /** 참가 확정됨 */
    CONFIRMED("확정됨"),

    /** 참가 취소됨 */
    CANCELLED("취소됨"),
    ;

    fun canConfirm(): Boolean = this == APPLIED

    fun canCancel(): Boolean = this in listOf(APPLIED, CONFIRMED)
}
