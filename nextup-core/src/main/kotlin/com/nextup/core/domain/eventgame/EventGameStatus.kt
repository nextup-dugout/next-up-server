package com.nextup.core.domain.eventgame

/**
 * 이벤트 게임 상태
 */
enum class EventGameStatus(
    val displayName: String,
) {
    /** 참가자 모집 중 */
    RECRUITING("모집 중"),

    /** 모집 마감 */
    CLOSED("모집 마감"),

    /** 팀 배정 완료 */
    TEAM_ASSIGNED("팀 배정 완료"),

    /** 경기 진행 중 */
    IN_PROGRESS("진행 중"),

    /** 경기 종료 */
    FINISHED("종료"),

    /** 취소 */
    CANCELLED("취소"),
    ;

    fun canClose(): Boolean = this == RECRUITING

    fun canAssignTeams(): Boolean = this == CLOSED

    fun canStart(): Boolean = this == TEAM_ASSIGNED

    fun canFinish(): Boolean = this == IN_PROGRESS

    fun canCancel(): Boolean = this in listOf(RECRUITING, CLOSED, TEAM_ASSIGNED)

    fun canJoin(): Boolean = this == RECRUITING

    fun isCompleted(): Boolean = this in listOf(FINISHED, CANCELLED)
}
