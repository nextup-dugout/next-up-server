package com.nextup.core.domain.stadium

/**
 * 구장 예약 양도 상태
 */
enum class TransferStatus {
    /**
     * 양도 등록 (양수 대기 중)
     */
    OPEN,

    /**
     * 양도 완료 (양수 확정)
     */
    ACCEPTED,

    /**
     * 양도 취소
     */
    CANCELLED,

    /**
     * 양도 만료 (유효기간 초과)
     */
    EXPIRED,
}
