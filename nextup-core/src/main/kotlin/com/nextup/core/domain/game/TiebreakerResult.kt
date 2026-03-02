package com.nextup.core.domain.game

/**
 * 이닝 전환 시 타이브레이크/이닝 제한 처리 결과
 */
enum class TiebreakerResult {
    /** 일반 이닝 전환 (타이브레이크 미적용) */
    NORMAL,

    /** 연장전 타이브레이크 적용 (무사 1,2루 자동 배치) */
    TIEBREAKER_APPLIED,

    /** 최대 연장 이닝 도달로 인한 무승부 처리 */
    DRAW_BY_INNINGS_LIMIT,
}
