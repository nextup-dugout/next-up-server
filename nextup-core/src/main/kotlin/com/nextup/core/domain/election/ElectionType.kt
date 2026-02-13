package com.nextup.core.domain.election

/**
 * 선거 유형
 */
enum class ElectionType {
    /**
     * 구단주 선출
     */
    OWNER_ELECTION,

    /**
     * 주장 선출
     */
    CAPTAIN_ELECTION,

    /**
     * 일반 투표
     */
    GENERAL,
}
