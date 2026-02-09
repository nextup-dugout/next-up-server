package com.nextup.core.domain.stadium

/**
 * 구장 슬롯 상태
 */
enum class SlotStatus {
    /**
     * 예약 가능
     */
    AVAILABLE,

    /**
     * 예약됨
     */
    BOOKED,

    /**
     * 유지보수 중
     */
    MAINTENANCE,
}
