package com.nextup.core.domain.stadium

/**
 * 구장 예약 양도 상태
 */
enum class TransferStatus {
    /**
     * 양도 요청 대기 중
     */
    PENDING,

    /**
     * 양도 수락 완료
     */
    ACCEPTED,

    /**
     * 양도 거절
     */
    REJECTED,
}
