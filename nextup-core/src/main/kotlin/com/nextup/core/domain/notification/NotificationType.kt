package com.nextup.core.domain.notification

/**
 * 알림 타입
 */
enum class NotificationType {
    /**
     * 경기 시작 알림
     */
    GAME_START,

    /**
     * 팀 공지사항
     */
    TEAM_NOTICE,

    /**
     * 출석 독려
     */
    ATTENDANCE_NUDGE,

    /**
     * 기록 알림
     */
    RECORD_ALERT,
}
