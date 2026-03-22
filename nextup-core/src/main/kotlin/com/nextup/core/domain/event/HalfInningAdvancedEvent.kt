package com.nextup.core.domain.event

/**
 * 반이닝 진행 도메인 이벤트
 *
 * 이닝이 전환될 때(초→말 또는 말→다음 이닝 초) 발행됩니다.
 * WebSocket 브로드캐스트를 통해 이닝 변경을 실시간으로 알립니다.
 *
 * @param gameId 경기 ID
 * @param newInning 새로운 이닝 번호
 * @param newIsTopInning 새로운 이닝이 초(true)인지 말(false)인지 여부
 */
data class HalfInningAdvancedEvent(
    val gameId: Long,
    val newInning: Int,
    val newIsTopInning: Boolean,
)
