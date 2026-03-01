package com.nextup.core.domain.competition

/**
 * 동점 경기 처리 방법
 */
enum class TiedGameResult {
    /** 무승부로 처리 */
    DRAW,

    /** 타이브레이커(연장전)로 승부 결정 */
    TIEBREAKER,

    /** 재경기로 승부 결정 */
    RESCHEDULE,
}
