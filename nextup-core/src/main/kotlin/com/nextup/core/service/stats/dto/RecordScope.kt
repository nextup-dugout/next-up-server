package com.nextup.core.service.stats.dto

/**
 * 기록 조회 범위
 *
 * 선수 기록을 조회할 때 시즌별, 통산, 대회별 등의 범위를 지정합니다.
 */
enum class RecordScope {
    /**
     * 시즌별 통계
     */
    SEASON,

    /**
     * 통산 통계
     */
    CAREER,

    /**
     * 대회별 통계
     */
    COMPETITION,
}
