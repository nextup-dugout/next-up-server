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

    /**
     * 경기 취소
     */
    GAME_CANCELLED,

    /**
     * 경기 일정 변경
     */
    GAME_RESCHEDULED,

    /**
     * 경기 연기
     */
    GAME_POSTPONED,

    /**
     * 팀원 탈퇴
     */
    TEAM_MEMBER_LEFT,

    /**
     * 팀원 강퇴
     */
    TEAM_MEMBER_KICKED,

    /**
     * 기록 정정
     */
    RECORD_CORRECTED,

    /**
     * 대회 완료
     */
    COMPETITION_COMPLETED,

    /**
     * 재선거(결선투표) 생성
     */
    RUNOFF_ELECTION_CREATED,
}
