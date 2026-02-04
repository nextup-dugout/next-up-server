package com.nextup.core.service.stats.dto

/**
 * 기록 조회 타입
 *
 * 타격, 투수, 전체 기록을 선택합니다.
 */
enum class RecordType {
    /**
     * 타격 기록만
     */
    BATTING,

    /**
     * 투수 기록만
     */
    PITCHING,

    /**
     * 전체 기록 (타격 + 투수)
     */
    ALL,
}
