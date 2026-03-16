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

    /**
     * 팀 가입 승인
     */
    TEAM_JOIN_APPROVED,

    /**
     * 팀 가입 거절
     */
    TEAM_JOIN_REJECTED,

    /**
     * 출석 투표 생성
     */
    ATTENDANCE_VOTE_CREATED,

    /**
     * 경기 결과 확정
     */
    GAME_RESULT_CONFIRMED,

    /**
     * 라인업 확정
     */
    LINEUP_CONFIRMED,

    /**
     * 선거 동률 발생
     */
    ELECTION_TIED,
}
