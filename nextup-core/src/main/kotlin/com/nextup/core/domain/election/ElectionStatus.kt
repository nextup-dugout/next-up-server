package com.nextup.core.domain.election

/**
 * 선거 상태
 */
enum class ElectionStatus {
    /**
     * 예정됨
     */
    SCHEDULED,

    /**
     * 진행 중
     */
    IN_PROGRESS,

    /**
     * 완료됨
     */
    COMPLETED,

    /**
     * 취소됨
     */
    CANCELLED,
}
