package com.nextup.core.domain.stadium

/**
 * 구장 예약 상태
 */
enum class BookingStatus {
    /**
     * 예약 확정
     */
    CONFIRMED,

    /**
     * 취소됨
     */
    CANCELLED,

    /**
     * 완료됨
     */
    COMPLETED,
}
