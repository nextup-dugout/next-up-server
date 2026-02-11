package com.nextup.core.domain.game

/**
 * 경기 상태
 */
enum class GameStatus(
    val displayName: String,
    val description: String,
) {
    /** 경기 예정 */
    SCHEDULED("예정", "경기가 예정된 상태"),

    /** 경기 진행 중 */
    IN_PROGRESS("진행 중", "경기가 진행 중인 상태"),

    /** 경기 종료 */
    FINISHED("종료", "경기가 정상적으로 종료된 상태"),

    /** 경기 취소 */
    CANCELLED("취소", "경기가 취소된 상태"),

    /** 경기 연기 */
    POSTPONED("연기", "우천 등의 사유로 경기가 연기된 상태"),

    /** 경기 몰수 */
    FORFEITED("몰수", "몰수 처리된 경기"),

    /** 콜드게임 */
    CALLED("콜드게임", "점수차 또는 기상 조건으로 조기 종료된 경기"),
    ;

    /**
     * 경기가 시작 가능한 상태인지 확인합니다.
     */
    fun canStart(): Boolean = this == SCHEDULED

    /**
     * 경기가 진행 중인 상태인지 확인합니다.
     */
    fun isOngoing(): Boolean = this == IN_PROGRESS

    /**
     * 경기가 완료된 상태인지 확인합니다 (정상 종료, 콜드게임, 몰수 포함).
     */
    fun isCompleted(): Boolean = this in listOf(FINISHED, CALLED, FORFEITED)

    /**
     * 경기가 취소/연기된 상태인지 확인합니다.
     */
    fun isCancelledOrPostponed(): Boolean = this in listOf(CANCELLED, POSTPONED)
}
