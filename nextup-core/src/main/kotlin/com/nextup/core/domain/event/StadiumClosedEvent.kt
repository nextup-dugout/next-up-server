package com.nextup.core.domain.event

/**
 * L-12: 구장 폐업(비활성화) 이벤트
 *
 * 구장이 비활성화(폐업)되었을 때 발행됩니다.
 * 해당 구장의 기존 예약을 일괄 취소하는 리스너에서 사용됩니다.
 */
data class StadiumClosedEvent(
    val stadiumId: Long,
    val stadiumName: String,
)
